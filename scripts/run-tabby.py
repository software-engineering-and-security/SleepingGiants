import json
import os
import shutil
import sys
import subprocess

from neo4j import GraphDatabase

DB_PROPERTIES_TEMPLATE = '''
tabby.cache.isDockerImportPath            = true
tabby.neo4j.username                      = neo4j
tabby.neo4j.password                      = password
tabby.neo4j.url                           = bolt://127.0.0.1:${BOLT_PORT}
'''


def launch_neo4j(command, container_count):
    print("Have you built?  docker build -t tabby_neo4j .")

    for i in range(0, container_count):
        container_name = f"neo4j_{i}"

        if command == "start":
            bolt_port = str(10000 + i)

            os.mkdir(os.path.join("tabby", str(i)))
            os.symlink(os.path.join(os.getcwd(), "tabby.jar"), os.path.join(os.getcwd(), "tabby", str(i), "tabby.jar"))
            os.symlink(os.path.join(os.getcwd(), "tabby-vul-finder.jar"),
                       os.path.join(os.getcwd(), "tabby", str(i), "tabby-vul-finder.jar"))
            os.symlink(os.path.join(os.getcwd(), "tabby_rules"), os.path.join(os.getcwd(), "tabby", str(i), "rules"))
            os.symlink(os.path.join(os.getcwd(), "cyphers.yml"),
                       os.path.join(os.getcwd(), "tabby", str(i), "cyphers.yml"))
            os.mkdir(os.path.join("tabby", str(i), "config"))
            os.mkdir(os.path.join("tabby", str(i), "import"))

            with open(os.path.join("tabby", str(i), "config", "db.properties"), "w") as db_properties:
                db_properties.write(DB_PROPERTIES_TEMPLATE.replace("${BOLT_PORT}", bolt_port))
            shutil.copyfile("settings.properties", os.path.join("tabby", str(i), "config", "settings.properties"))

            subprocess.run(
                ["docker", "run", "--name", container_name,
                 "-v", f"{os.path.join(os.getcwd(), 'tabby', str(i), 'import')}:/var/lib/neo4j/import",
                 "--rm",
                 "-p", f"{bolt_port}:7687",
                 "-e", "NEO4J_AUTH=neo4j/password",
                 "-e", "NEO4J_dbms_security_procedures_unrestricted=jwt.security.*,apoc.*",
                 "-e", "NEO4J_server_memory_heap_initial__size=1G",
                 "-e", "NEO4J_server_memory_heap_max__size=4G",
                 "-e", "NEO4J_server_memory_pagecache_size=4G"
                       "-t", "-d", "tabby_neo4j", ])

        if command == "kill":
            subprocess.run(["docker", "stop", container_name])
            if os.path.exists(os.path.join("tabby", str(i))):
                shutil.rmtree(os.path.join("tabby", str(i)))


JDK_PACKAGE_BASES = ["java", "javax", "sun", "jdk", "ses"]


def run_cypher(container_index):
    uri = f"neo4j://localhost:{10000 + container_index}"
    driver = GraphDatabase.driver(uri, auth=("neo4j", "password"), max_connection_lifetime=100)

    records, _, _ = driver.execute_query(
        'match (source:Method {IS_SERIALIZABLE: true}) '
        'WHERE ANY (n IN source.NAME WHERE n IN ["readObject", "readObjectNoData", "readResolve", "readExternal"])'
        'match (sink:Method {IS_SINK: true}) '
        'call apoc.algo.allSimplePaths(sink, source, "<", 15) yield path '
        'where none(n in nodes(path) where n.CLASSNAME in ["java.io.ObjectInputStream"])'
        'and all(n in nodes(path) where n.IS_SERIALIZABLE <> true)'
        'or single(n in nodes(path) where n.IS_SERIALIZABLE <> false)'
        'return nodes(path), relationships(path)', database_="neo4j"
    )

    chains = []

    for record in records:
        nodes = record["nodes(path)"]
        chain = []
        contains_only_jdk = True
        for node in nodes:
            chain.append(node['SIGNATURE'])
            if node['SIGNATURE'].split(".")[0].replace("<", "") not in JDK_PACKAGE_BASES:
                contains_only_jdk = False

        if not contains_only_jdk:
            chains.append(chain[::-1])
    driver.close()
    return chains


def run_vul_finder(container_index):
    print("[INFO] run vul finder")

    result_dir = os.path.join("tabby", str(container_index), "result")

    if os.path.exists(result_dir):
        shutil.rmtree(result_dir)

    subprocess.run(["java", "-jar", "tabby-vul-finder.jar", "query", ".", "cyphers.yml"],
                   cwd=os.path.join("tabby", str(container_index)))

    result_sub_dir = os.path.join(result_dir, os.listdir(result_dir)[0])
    result_file = os.path.join(result_sub_dir, os.listdir(result_sub_dir)[0])

    start_comment = False
    chains = []
    chain = []

    for line in open(result_file, "r").read().split("\n"):
        line = line.strip()
        if line == "/*":
            start_comment = True
            continue

        if line == "*/":
            start_comment = False
            if len(chain) > 0:
                chains.append(chain)
                chain = []
            continue

        if start_comment:
            chain.append(line)

    print(chains)

    return chains


def get_target_dir(target):
    if target == "unmodified":
        return os.path.join("..", "input", "libs")
    if target in ["a", "b", "c", "all", "all_B"]:
        return os.path.join("..", "output", f"modification_{target}")
    exit(1)


def peek(target):
    result_dir = os.path.join("..", "output", "results", "tabby", target)
    for file in os.listdir(result_dir):
        chains = json.load(open(os.path.join(result_dir, file), "r"))
        if len(chains) > 0:
            print(file)


def run(dependency, container_index, target="unmodified"):
    target_dir = get_target_dir(target)

    shutil.copyfile(os.path.join(target_dir, dependency), os.path.join("tabby", str(container_index), "target.jar"))
    subprocess.run(["java", "-jar", "tabby.jar"], cwd=os.path.join("tabby", str(container_index)))

    output_dir = os.path.join("tabby", str(container_index), "output", "dev")
    for file in os.listdir(output_dir):
        shutil.copyfile(os.path.join(output_dir, file), os.path.join("tabby", str(container_index), "import", file))

    # java -jar tabby-vul-finder.jar load $2
    subprocess.run(["java", "-jar", "tabby-vul-finder.jar", "load", "."],
                   cwd=os.path.join("tabby", str(container_index)))

    # result = run_cypher(container_index)
    result = run_vul_finder(container_index)

    result_dir = os.path.join("..", "output", "results", "tabby", target)
    os.makedirs(result_dir, exist_ok=True)
    json.dump(result, open(os.path.join(result_dir, f"{dependency[:-4]}.json"), "w"))


def contains_non_jdk_gadgets(chain):
    for gadget in chain:
        package_base = gadget.replace("-[CALL]->", "").strip().split(".")[0]
        if package_base not in JDK_PACKAGE_BASES:
            return True
        if "-[ALIAS]->" in gadget:
            gadget2 = gadget.split("-[ALIAS]->")[1]
            package_base = gadget2.strip().split(".")[0]
            if package_base not in JDK_PACKAGE_BASES:
                return True

    return False


def compare(target):
    unmodified_dir = os.path.join("..", "output", "results", "tabby", "unmodified")
    target_dir = os.path.join("..", "output", "results", "tabby", target)

    output_dir = os.path.join("..", "output", "results", "tabby", "parsed", target)
    if not os.path.exists(output_dir):
        os.makedirs(output_dir, exist_ok=True)

    diff_cnt = 0

    for file in os.listdir(target_dir):
        target_chains = json.load(open(os.path.join(target_dir, file)))
        unmodified_chains = json.load(open(os.path.join(unmodified_dir, file)))

        diff_mod = []
        diff_orig = []

        for chain in target_chains:
            if contains_non_jdk_gadgets(chain) and chain not in unmodified_chains:
                diff_mod.append(chain)

        for chain in unmodified_chains:
            if contains_non_jdk_gadgets(chain) and chain not in target_chains:
                diff_orig.append(chain)

        if len(diff_mod) > 0:
            diff_cnt = diff_cnt + 1
            output_obj = {
                "orig_cnt" : len(unmodified_chains),
                "modified_cnt": len(target_chains),
                "diff_mod" : diff_mod,
                "diff_orig": diff_orig
            }
            json.dump(output_obj, open(os.path.join(output_dir, file), "w"))

    print(f"Diff count: {diff_cnt}")
    return


def main():
    if sys.argv[1] == "neo4j":
        launch_neo4j(sys.argv[2], int(sys.argv[3]))

    if sys.argv[1] == "run":
        run(sys.argv[2], int(sys.argv[3]), sys.argv[4])

    if sys.argv[1] == "compare":
        compare(sys.argv[2])

    if sys.argv[1] == "query":
        print(run_cypher(int(sys.argv[2])))

    if sys.argv[1] == "peek":
        print(peek(sys.argv[2]))


if __name__ == "__main__":
    main()

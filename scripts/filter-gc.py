import os
import sys
import json

RESULT_DIR_ANDROCHAIN = os.path.join("..", "output", "results", "androchain")
RESULT_DIR_TABBY = os.path.join("..", "output", "results", "tabby", "parsed")
RESULT_DIR_CRYSTALLIZER = os.path.join("..", "output", "results", "crystallizer", "parsed")

INTERESTING_SINK_METHODS = ['java.lang.reflect.Method.invoke', 'java.lang.ClassLoader.defineClass', 'org.springframework.jndi.JndiTemplate.lookup', 'java.sql.PreparedStatement.execute',
                            'java.io.FileOutputStream.write', 'java.lang.ClassLoader.loadClass', 'java.net.URL.openConnection', 'java.sql.Statement.execute', 'javax.naming.InitialContext.lookup',
                            'java.lang.reflect.Constructor.newInstance',  'java.io.FileOutputStream.<init>',  'java.sql.PreparedStatement.executeQuery',
                            'java.io.File.delete', 'java.beans.Introspector.getBeanInfo', 'java.net.URL.openStream',   'java.sql.DriverManager.getConnection', 'java.sql.Connection.prepareStatement',
                            'java.nio.file.Files.newOutputStream',  'javax.naming.Context.lookup',  'java.lang.ProcessBuilder.<init>', 'java.lang.Runtime.exec', 'java.rmi.registry.Registry.lookup',
                            'java.nio.file.Files.newBufferedWriter']


def get_sink_gadget(tool, gadget_chain):
    sink_gadget = gadget_chain[-1]
    method_name, class_name = "", ""

    if tool == RESULT_DIR_TABBY:
        if "[ALIAS]" in sink_gadget:
            sink_gadget = sink_gadget.split("-[ALIAS]->")[1]
        sink_gadget = sink_gadget.replace("-[CALL]->", "").strip()
        method_name = sink_gadget.split("#")[1]
        class_name = sink_gadget.split("#")[0]

    elif tool == RESULT_DIR_ANDROCHAIN or tool == RESULT_DIR_CRYSTALLIZER:
        method_name = sink_gadget.split(" ")[2].split("(")[0]
        class_name = sink_gadget.split(":")[0].replace("<", "")

    return class_name + "." + method_name


def print_gc_cnt(result_dir):
    unique_gcs = []

    for directory in os.listdir(result_dir):
        subdir = os.path.join(result_dir, directory)
        if not os.path.isdir(subdir): continue

        for file in os.listdir(subdir):
            if not ".json" in file: continue

            gc_obj = json.load(open(os.path.join(subdir, file), "r"))
            if "diff_mod" in gc_obj:
                for chain in gc_obj["diff_mod"]:
                    if not chain in unique_gcs:
                        unique_gcs.append(chain)

    return unique_gcs


def get_sinks():
    sink_gadget_dict = {}

    for result_dir in [RESULT_DIR_TABBY, RESULT_DIR_CRYSTALLIZER, RESULT_DIR_ANDROCHAIN]:
        chains = print_gc_cnt(result_dir)
        print(f"{result_dir} : {len(chains)}")

        for chain in chains:
            sink_gadget = get_sink_gadget(result_dir, chain)

            if not sink_gadget in sink_gadget_dict:
                sink_gadget_dict[sink_gadget] = 0
            sink_gadget_dict[sink_gadget] += 1

    print(sink_gadget_dict)

    for key in sink_gadget_dict:
        print(f"{key} : {sink_gadget_dict[key]}")


def dir_to_key(directory):
    if directory == RESULT_DIR_ANDROCHAIN: return "androChain"
    if directory == RESULT_DIR_CRYSTALLIZER: return "crystallizer"
    if directory == RESULT_DIR_TABBY: return "tabby"


def filter_chains():

    output_dir = os.path.join("..", "output", "results", "filtered")
    if not os.path.exists(output_dir):
        os.makedirs(output_dir, exist_ok=True)

    output_dict = {}

    for result_dir in [RESULT_DIR_TABBY, RESULT_DIR_CRYSTALLIZER, RESULT_DIR_ANDROCHAIN]:

        tool = dir_to_key(result_dir)

        for directory in os.listdir(result_dir):
            subdir = os.path.join(result_dir, directory)
            if not os.path.isdir(subdir): continue

            for file in os.listdir(subdir):
                if not ".json" in file: continue

                dependency_name = (file.replace(".json", "").replace("_all_B", "")
                                   .replace("_all", "").replace("_a", "")
                                   .replace("_b", "").replace("_c", ""))

                gc_obj = json.load(open(os.path.join(subdir, file), "r"))

                interesting_gcs = []
                if "diff_mod" in gc_obj:
                    for chain in gc_obj["diff_mod"]:
                        if not get_sink_gadget(result_dir, chain) in INTERESTING_SINK_METHODS:
                            continue

                        if not chain in interesting_gcs:
                            interesting_gcs.append(chain)

                if len(interesting_gcs) > 0:
                    if not dependency_name in output_dict:
                        output_dict[dependency_name] = {
                            "tabby": [], "androChain": []
                        }

                    for chain in interesting_gcs:
                        if not chain in output_dict[dependency_name][tool]:
                            output_dict[dependency_name][tool].append(chain)

    print(len(output_dict))
    for dependency in output_dict:
        json.dump(output_dict[dependency], open(os.path.join(output_dir, f"{dependency}.json"), "w"), indent= 4)

    return


def batch_stats():
    filtered_dir = os.path.join("..", "output", "results", "filtered")
    for file in os.listdir(filtered_dir):
        if not ".json" in file: continue

        obj = json.load(open(os.path.join(filtered_dir, file), "r"))
        print(f"{file}; {len(obj['androChain']) + len(obj['tabby'])}")

    return


def main():
    if sys.argv[1] == "sinks":
        get_sinks()
    if sys.argv[1] == "filter":
        filter_chains()

    if sys.argv[1] == "stats":
        batch_stats()


if __name__ == "__main__":
    main()



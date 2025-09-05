import sys
import subprocess
import os
import shutil
import json

DEFAULT_DEPENDENCY_DIR = os.path.join("..", "input", "libs")


def launch_crystallizer(container_count):
    if not os.path.exists("crystallizer"):
        os.mkdir("crystallizer")

    subprocess.run(["docker", "pull", "prashast94/crystallizer:latest"])

    for i in range(0, container_count):

        container_name = f"crystallizer_{i}"
        if os.path.exists(os.path.join("crystallizer", str(i))):
            shutil.rmtree(os.path.join("crystallizer", str(i)))
        os.mkdir(os.path.join("crystallizer", str(i)))

        results_dir = os.path.join("crystallizer", str(i), "results")
        target_dir = os.path.join("crystallizer", str(i), "target")
        os.mkdir(results_dir)
        os.mkdir(target_dir)

        subprocess.run(["docker", "run", "--security-opt", "seccomp=unconfined", "--name", container_name, "-v",
                        f"{os.path.join(os.getcwd(), target_dir)}:/root/SeriFuzz/targets", "-v",
                        f"{os.path.join(os.getcwd(), results_dir)}:/root/SeriFuzz/results", "--rm",
                        "-t", "-d", "prashast94/crystallizer:latest"])


def run_crystallizer(batch_file, offset=0, dependency_dir=DEFAULT_DEPENDENCY_DIR):
    container_index = offset

    for line in open(batch_file, "r").readlines():
        line = line.strip()
        if line == "": continue

        dependency = line
        target_dir = os.path.join("crystallizer", str(container_index), "target")
        container_name = f"crystallizer_{container_index}"

        if not os.path.exists(os.path.join(target_dir, dependency.replace(".jar", ""))):
            os.mkdir(os.path.join(target_dir, dependency.replace(".jar", "")))

        original_dependency = os.path.join(dependency_dir, dependency)
        shutil.copyfile(original_dependency, os.path.join(target_dir, dependency.replace(".jar", ""), dependency))
        subprocess.run(["docker", "exec", container_name, "rm", "-r", "/root/SeriFuzz/results/concretization/crashes"])
        subprocess.run(["docker", "exec", container_name, "rm", "-r",
                        "/root/.cache/bazel/_bazel_root/install/c87283ec3a7822eea44f4cecb6db792e"])
        subprocess.run(["docker", "exec", "-d", container_name, "./eval/run_campaigns.sh",
                        f"/root/SeriFuzz/targets/{dependency.replace('.jar', '')}/{dependency}", "1h", "24h"])
        container_index += 1

    return


def peek(container_count):
    for i in range(0, container_count):
        results_dir = os.path.join("crystallizer", str(i), "results")
        target_dir = os.path.join('crystallizer', str(i), 'target')
        target_lib = os.listdir(target_dir)[0]

        concretization_dir = os.path.join(results_dir, "concretization")
        if os.path.exists(concretization_dir):
            if len(os.listdir(concretization_dir)) > 1:
                print(f"{i} : {target_lib}")


def get_exploitable(container_count):
    for i in range(0, container_count):
        results_dir = os.path.join("crystallizer", str(i), "results")
        target_dir = os.path.join('crystallizer', str(i), 'target')
        target_lib = os.listdir(target_dir)[0]

        sinkID_dir = os.path.join(results_dir, "sinkID")
        if os.path.exists(sinkID_dir) and os.path.exists(os.path.join(sinkID_dir, "potential_sinks_exploitable")):
            if open(os.path.join(sinkID_dir, "potential_sinks_exploitable"), "r").read().strip() != "":
                print(f"{i} : {target_lib}")


def kill_crystallizer(container_count):
    for i in range(0, container_count):
        container_name = f"crystallizer_{i}"
        subprocess.run(["docker", "stop", container_name])

    return


def store(container_count, save_dir, offset = 0):
    os.makedirs(save_dir, exist_ok=True)

    for i in range(offset, offset + container_count):
        container_name = f"crystallizer_{str(i)}"
        results_dir = os.path.join("crystallizer", str(i), "results")
        target_dir = os.path.join('crystallizer', str(i), 'target')
        target_lib = os.listdir(target_dir)[0]

        subprocess.run(["docker", "exec", container_name, "python", "/root/SeriFuzz/eval/concretized_paths.py",
                        "--concretized_ids", "/root/SeriFuzz/jazzer/crashes", "--paths",
                        "/root/SeriFuzz/jazzer/candidate_paths"])
        subprocess.run(["docker", "exec", container_name, "cp", "/root/SeriFuzz/concretized_paths.json",
                        "/root/SeriFuzz/results/concretized_paths.json"])

        save_lib_dir = os.path.join(save_dir, target_lib)

        for dir in ["sinkID", "concretization"]:
            if not os.path.exists(os.path.join(results_dir, dir)): continue

            if os.path.exists(os.path.join(save_lib_dir, dir)):
                shutil.rmtree(os.path.join(save_lib_dir, dir))
            shutil.copytree(os.path.join(results_dir, dir), os.path.join(save_lib_dir, dir))

        shutil.copyfile(os.path.join(results_dir, "concretized_paths.json"),
                        os.path.join(save_lib_dir, "concretized_paths.json"))


def get_results(results_directory):
    static_res = []
    dynamic_res = []

    candidate_paths_path = os.path.join(results_directory, "concretization", "candidate_paths")
    concretized_paths_path = os.path.join(results_directory, "concretized_paths.json")

    if os.path.exists(candidate_paths_path):
        for line in open(candidate_paths_path, "r").read().split("\n"):
            line = line.strip()
            if ":: ->" in line:
                static_res.append(line.split("->")[1:])

    if os.path.exists(concretized_paths_path):
        fuzz_dict = json.load(open(concretized_paths_path, "r"))['lib_paths_name']
        for id in fuzz_dict:

            chain = fuzz_dict[id].split("->")[1:]
            if not chain in dynamic_res:
                dynamic_res.append(chain)

    return static_res, dynamic_res


JDK_PACKAGE_BASES = ["java", "javax", "sun", "jdk", "ses"]


def compare(target, unmodified_dir=os.path.join("..", "output", "results", "crystallizer", "unmodified")):

    crystallizer_result_dir = os.path.join("..", "output", "results", "crystallizer")
    target_dir = os.path.join(crystallizer_result_dir, target)

    output_dir = os.path.join("..", "output", "results", "crystallizer", "parsed", target)
    if not os.path.exists(output_dir):
        os.makedirs(output_dir, exist_ok=True)

    for dependency in os.listdir(target_dir):
        static_res, dynamic_res = get_results(os.path.join(target_dir, dependency))
        static_res_unmodified, dynamic_res_unmodified = get_results(os.path.join(unmodified_dir, dependency))

        diff_mod = []
        diff_orig = []

        for chain in dynamic_res:
            is_only_jdk = True

            for gadget in chain:
                package_base = gadget.replace("<", "").split(".")[0]
                if package_base not in JDK_PACKAGE_BASES:
                    is_only_jdk = False
                    break

            if not is_only_jdk and chain not in dynamic_res_unmodified:
                diff_mod.append(chain)

        for chain in dynamic_res_unmodified:
            is_only_jdk = True

            for gadget in chain:
                package_base = gadget.replace("<", "").split(".")[0]
                if package_base not in JDK_PACKAGE_BASES:
                    is_only_jdk = False
                    break

            if not is_only_jdk and chain not in dynamic_res:
                diff_orig.append(chain)

        if len(diff_mod) > 0:
            output_obj = {
                "orig_cnt": len(dynamic_res_unmodified),
                "modified_cnt": len(dynamic_res),
                "diff_mod": diff_mod,
                "diff_orig": diff_orig
            }

            json.dump(output_obj, open(os.path.join(output_dir, f"{dependency}.json")), indent=4)





def main():
    if sys.argv[1] == "kill":
        kill_crystallizer(int(sys.argv[2]))
    elif sys.argv[1] == "start":
        launch_crystallizer(int(sys.argv[2]))
    elif sys.argv[1] == "run":
        if len(sys.argv) > 3:
            run_crystallizer(sys.argv[2], int(sys.argv[3]), sys.argv[4])
        else:
            run_crystallizer(sys.argv[2])

    elif sys.argv[1] == "peek":
        peek(int(sys.argv[2]))

    elif sys.argv[1] == "exploitable":
        get_exploitable(int(sys.argv[2]))

    elif sys.argv[1] == "store":

        if sys.argv[4]:
            store(int(sys.argv[2]), sys.argv[3], int(sys.argv[4]))
        else:
            store(int(sys.argv[2]), sys.argv[3])
    elif sys.argv[1] == "compare":
        compare(sys.argv[2])

    return


if __name__ == "__main__":
    main()

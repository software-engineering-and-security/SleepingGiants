import os
import subprocess
import sys
import json


def run_androChain(dependency):
    result_dir = os.path.join("..", "output", "results", "androchain")

    if not os.path.exists(result_dir):
        os.makedirs(result_dir, exist_ok=True)

    result_file = os.path.join(result_dir, dependency.split(os.path.sep)[-1].replace(".jar", "") + ".txt")
    return_code = subprocess.call(["java",  "-jar", "androChain-1.0.jar", "--jar", dependency, "--serializableCheck",
                                   "-d", "100", "--sinkList", "sinks_tabby.txt", "--from", "all_serializable", "--output", result_file], stdout=subprocess.DEVNULL)

    result = []

    if return_code == 0:
        result_file_fp = open(result_file, "r")
        entry = []
        for line in result_file_fp.read().split("\n"):
            line = line.strip()
            if line == "":
                if len(entry) > 0:
                    result.append(entry)
                    entry = []
            else:
                entry.append(line)

    return result


def main():
    original_dependency = os.path.join("..", "input", "libs", sys.argv[1])
    modified_dependency = os.path.join("..", "output", "modification_" + sys.argv[2], sys.argv[1])

    if not os.path.exists(original_dependency):
        print(f"[ERROR] {original_dependency} does not exist")
        exit(1)
    if not os.path.exists(modified_dependency):
        print(f"[ERROR] {modified_dependency} does not exist")
        exit(1)

    result_orig = run_androChain(original_dependency)
    result_mod = run_androChain(modified_dependency)

    print(len(result_orig))
    print(len(result_mod))

    diff_mod = []
    diff_orig = []

    for entry_mod in result_mod:
        uses_only_jdk = True
        for gadget in entry_mod:
            package_base = gadget.split(".")[0].replace("<", "")
            # we remove our ses trampoline caller as well, since we are not interested in gcs leading to JCL
            if package_base not in ["java", "javax", "sun", "ses", "jdk"]:
                uses_only_jdk = False

        if not uses_only_jdk:
            if entry_mod not in result_orig:
                diff_mod.append(entry_mod)

    for entry_orig in result_orig:
        uses_only_jdk = True
        for gadget in entry_orig:
            package_base = gadget.split(".")[0].replace("<", "")
            # we remove our ses trampoline caller as well, since we are not interested in gcs leading to JCL
            if package_base not in ["java", "javax", "sun", "ses", "jdk"]:
                uses_only_jdk = False

        if not uses_only_jdk:
            if entry_orig not in result_mod:
                diff_orig.append(entry_orig)

    if len(diff_mod) > 0:
        output_dict = { "orig_cnt" : len(result_orig), "modified_cnt" : len(result_mod), "diff_mod" : diff_mod, "diff_orig" : diff_orig}

        result_file = os.path.join("..", "output", "results", "androchain", sys.argv[1].replace(".jar", "") + "_" + sys.argv[2] + ".json")
        json.dump(output_dict, open(result_file, "w"), indent=4)


if __name__ == "__main__":
    main()

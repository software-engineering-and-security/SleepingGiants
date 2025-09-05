import os
import json
import statistics
import sys

LOG_DIR_A = os.path.join("..", "output", "modification_a", "log")
LOG_DIR_B = os.path.join("..", "output", "modification_b", "log")
LOG_DIR_C = os.path.join("..", "output", "modification_c", "log")

RESULT_DIR_ANDROCHAIN = os.path.join("..", "output", "results", "androchain")
RESULT_DIR_TABBY = os.path.join("..", "output", "results", "tabby", "parsed")
RESULT_DIR_CRYSTALLIZER = os.path.join("..", "output", "results", "crystallizer", "parsed")


def print_stats(log_directory):
    log_cnt, min, max, sum = 0, 999999999, 0, 0
    vals = []

    print(f"--------({log_directory})----------")
    for file in os.listdir(log_directory):
        if not ".json" in file: continue

        log_obj = json.load(open(os.path.join(LOG_DIR_A, file), "r"))
        if log_directory == LOG_DIR_A:
            cnt_serializable = len(log_obj["inserted_serializable"])
            if cnt_serializable > max:
                max = cnt_serializable
            if cnt_serializable < min:
                min = cnt_serializable

            sum += cnt_serializable
            vals.append(cnt_serializable)
        elif log_directory == LOG_DIR_B:
            cnt_constants = len(log_obj["inserted_constants"])

            if cnt_constants > max:
                max = cnt_constants
            if cnt_constants < min:
                min = cnt_constants
            sum += cnt_constants
            vals.append(cnt_constants)

        elif log_directory == LOG_DIR_C:
            cnt_iface = len(log_obj.keys()) - 1

            if cnt_iface > max:
                max = cnt_iface
            if cnt_iface < min:
                min = cnt_iface
            sum += cnt_iface
            vals.append(cnt_iface)

    print(f"Min: {min}")
    print(f"Max: {max}")
    print(f"Avg: {sum / log_cnt}")
    print(f"Deviation: {statistics.stdev(vals)}")


def print_injection_stats():
    print_stats(LOG_DIR_A)
    print_stats(LOG_DIR_B)
    print_stats(LOG_DIR_C)


IGNORE_DEPS = []


def get_dependencies_from_dir(directory) -> set:
    dependencies = set()
    #for line in open("Crystallizer_batches/batch_B", "r").read().split('\n'):
    #    IGNORE_DEPS.append(line.strip())
    #for line in open("Crystallizer_batches/batch_A", "r").read().split('\n'):
    #    IGNORE_DEPS.append(line.strip())

    for file in os.listdir(directory):
        if not ".json" in file: continue
        file_name = ""
        if "_" in file:
            file_name = f"{file.split('_')[0]}.jar"
        else:
            file_name = f"{file[:-5]}.jar"

        if file_name in IGNORE_DEPS:
            print(f"{file[:-5]}.jar")
            continue

        dependencies.add(file_name)

    return dependencies


def print_gc_lib_stats():
    unique_dependencies = set()

    for directory in os.listdir(RESULT_DIR_ANDROCHAIN):
        if not os.path.isdir(os.path.join(RESULT_DIR_ANDROCHAIN, directory)): continue
        unique_dependencies.update(get_dependencies_from_dir(os.path.join(RESULT_DIR_ANDROCHAIN, directory)))

    for directory in os.listdir(RESULT_DIR_TABBY):
        if not os.path.isdir(os.path.join(RESULT_DIR_TABBY, directory)): continue
        unique_dependencies.update(get_dependencies_from_dir(os.path.join(RESULT_DIR_TABBY, directory)))
    for directory in os.listdir(RESULT_DIR_CRYSTALLIZER):
        if not os.path.isdir(os.path.join(RESULT_DIR_CRYSTALLIZER, directory)): continue
        unique_dependencies.update(get_dependencies_from_dir(os.path.join(RESULT_DIR_CRYSTALLIZER, directory)))

    print(len(unique_dependencies))


def main():
    if sys.argv[1] == "inject":
        print_injection_stats()

    elif sys.argv[1] == "gc":
        print_gc_lib_stats()


if __name__ == "__main__":
    main()

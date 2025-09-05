import sys
import os
import subprocess
import shutil
import json
from tqdm import tqdm

INJECT_JAR = "gadget-inject.jar"
OUTPUT_DIR_SER = os.path.join("..", "output", "modification_a")
OUTPUT_DIR_FINAL = os.path.join("..", "output", "modification_b")
# below line commented for combining to all. We launched first from modification_a as input and with final, and then with modification_a_and_b as input and all
OUTPUT_DIR_TRAMPOLINE = os.path.join("..", "output", "modification_c")
OUTPUT_DIR_ALL = os.path.join("..", "output", "modification_all")

DOWNLOAD_DIR = sys.argv[1]
MODE = sys.argv[2]

if not os.path.exists(DOWNLOAD_DIR):
    print(f"[ERROR] path {DOWNLOAD_DIR} does not exist")
    exit(1)

if MODE not in ["all", "ser", "final", "trampoline"]:
    print(f"[ERROR] invalid mode {MODE}, not in [all, ser, final, trampoline]")
    exit(1)

output = ""
if MODE == "ser": output = OUTPUT_DIR_SER
if MODE == "final": output = OUTPUT_DIR_FINAL
if MODE == "trampoline": output = OUTPUT_DIR_TRAMPOLINE
if MODE == "all": output = OUTPUT_DIR_ALL

if not os.path.exists(output):
    os.mkdir(output)

log_dir = os.path.join(output, "log")
if not os.path.exists(log_dir):
    os.mkdir(log_dir)

if MODE == "ser" or MODE == "final" or MODE == "all":

    for file in tqdm(os.listdir(DOWNLOAD_DIR)):
        if not ".jar" in file:
            continue

        if os.path.exists(os.path.join(output, file)): continue

        proc = subprocess.run(
            ["java", "-jar", INJECT_JAR, os.path.join(DOWNLOAD_DIR, file), MODE],
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)

        log_json = ""
        json_output_start = False

        for line in proc.stdout.split("\n"):
            line = line.strip()
            if line == "{": json_output_start = True
            if not json_output_start: continue

            log_json += line + "\n"

        print(log_json)
        if log_json == "": log_json = "{}"

        json.dump(json.loads(log_json), open(os.path.join(log_dir, file.replace(".jar", ".json")), "w"), indent=4)
        print(proc.stderr)

        modified_jar_name = file.replace(".jar", "-modified.jar")
        shutil.move(os.path.join(DOWNLOAD_DIR, modified_jar_name), os.path.join(output, file))

elif MODE == "trampoline":

    trampoline_java_source_dir = os.path.join(os.path.join(OUTPUT_DIR_TRAMPOLINE, "src"))
    if not os.path.exists(trampoline_java_source_dir):
        os.mkdir(trampoline_java_source_dir)

    for file in tqdm(os.listdir(DOWNLOAD_DIR)):
        if not ".jar" in file:
            continue

        proc = subprocess.run(
            ["java", "-cp", INJECT_JAR, "ses.ginject.FindInterfacesMain", os.path.join(DOWNLOAD_DIR, file),
             os.path.join(trampoline_java_source_dir, "Caller.java")], text=True, stdout=subprocess.PIPE)

        log_json = ""
        json_output_start = False

        for line in proc.stdout.split("\n"):
            line = line.strip()
            if line.startswith("{"): json_output_start = True
            if not json_output_start: continue

            log_json += line + "\n"

        json.dump(json.loads(log_json), open(os.path.join(log_dir, file.replace(".jar", ".json")), "w"), indent=4)

        subprocess.run(["javac", os.path.join(trampoline_java_source_dir, "Caller.java")])

        proc = subprocess.run(
            ["java", "-jar", INJECT_JAR, os.path.join(DOWNLOAD_DIR, file), MODE,
             os.path.join(trampoline_java_source_dir, "Caller.class")],
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)

        print(proc.stderr)

        modified_jar_name = file.replace(".jar", "-modified.jar")
        shutil.move(os.path.join(DOWNLOAD_DIR, modified_jar_name), os.path.join(output, file))

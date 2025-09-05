import os
import pandas as pd
import requests
import zipfile
import subprocess
import sys
import shutil

#################
INPUT_FILE = os.path.join('input', 'clean_versions.csv')
ANALYSIS_JAR = 'jcl-serializable.jar'
OUTPUT_DIR = 'output'

if os.path.split(os.getcwd())[-1] == "scripts":
    INPUT_FILE = os.path.join('..', INPUT_FILE)
    OUTPUT_DIR = os.path.join('..', OUTPUT_DIR)
    ANALYSIS_JAR = os.path.join('..', ANALYSIS_JAR)

if not os.path.exists('tmp'):
    os.mkdir('tmp')
#################


def aar_to_jar(source, dest, dest_file_name):
    if os.path.exists(os.path.join(dest, dest_file_name)):
        return

    aar_tmp_dir = os.path.join(dest, dest_file_name.replace(".jar", ""))

    aar = zipfile.ZipFile(source)
    has_classes_file = False
    for file in aar.namelist():
        if 'classes.jar' in file:
            aar.extract(file, aar_tmp_dir)
            has_classes_file = True
            break

    if has_classes_file:
        shutil.move(os.path.join(aar_tmp_dir, 'classes.jar'), os.path.join(dest, dest_file_name))
    shutil.rmtree(aar_tmp_dir)

    return


def download_jar(download_url):
    file_name = download_url.split('/')[-1]
    file_path = os.path.join('tmp', file_name)

    if os.path.exists(file_path):
        return file_path

    response = requests.get(download_url, allow_redirects=True)
    if response.status_code > 299:
        return None

    open(file_path, 'wb').write(response.content)

    if file_name.endswith(".aar"):
        aar_to_jar(file_path, 'tmp', file_name.replace(".aar", ".jar"))
        os.remove(file_path)
        file_name = file_name.replace(".aar", ".jar")
        file_path = os.path.join('tmp', file_name)
        if not os.path.exists(file_path):
            print(f"[INFO] No classes.jar for {download_url}")
            return None

    # get rid of none-bytecode-jar files
    contains_java_bytecode = False
    jar = zipfile.ZipFile(file_path)
    for entry in jar.namelist():
        if '.class' in entry:
            contains_java_bytecode = True
            break
    jar.close()
    if not contains_java_bytecode:
        print(f"[INFO] No Java bytecode files in {download_url}")
        return None

    return file_path


def get_serializable_cnt(jar_file):
    if not os.path.exists(jar_file):
        return None

    proc = subprocess.run(["java", "-jar", ANALYSIS_JAR, jar_file], stdout=subprocess.PIPE, text=True)

    if proc.returncode == 0:
        for line in proc.stdout.split("\n"):
            if line.startswith("SERIALIZABLE_CNT"):
                serializable_cnt = line.replace("SERIALIZABLE_CNT", "").strip()
                return serializable_cnt
    return None


def get_all_stats(jar_file_1, jar_file_2):
    if not os.path.exists(jar_file_1) or not os.path.exists(jar_file_2) :
        return None

    proc = subprocess.run(["java", "-jar", ANALYSIS_JAR, jar_file_1, jar_file_2], stdout=subprocess.PIPE, text=True)

    if proc.returncode != 0:
        print(proc.stdout)

    if proc.returncode == 0:
        stats_dict = {"serializable_cnt" : 0, "add_serializable_indirect" : 0, "remove_serializable_indirect" : 0,
                      "add_serializable" : 0,"remove_serializable" : 0, "add_class" : 0, "remove_class" : 0}

        for line in proc.stdout.split("\n"):
            for key in stats_dict:
                if line.startswith(f"{key.upper()}:"):
                    stats_dict[key] = line.replace(f"{key.upper()}:", "").strip()
        return stats_dict

    return None


df = pd.read_csv(INPUT_FILE)

df['serializable_cnt'] = None
df['add_serializable'] = None
df['remove_serializable'] = None
df['add_serializable_indirect'] = None
df['remove_serializable_indirect'] = None
df['add_class'] = None
df['remove_class'] = None
df['next_version'] = None

if len(sys.argv) < 2:
    print("[ERROR] No depedency provided as argument")
    exit(1)

dependency = sys.argv[1]

df_dependency = df.loc[df['dependency_name'] == dependency].sort_values(by=['version'], key=lambda x: x.str.split('.').apply(lambda y: [int(i) for i in y]))
download_urls = df_dependency['download_url'].tolist()
versions = df_dependency['version'].tolist()

download_file_next = None

if len(download_urls) == 1:
    download_url = download_urls[0]
    download_file = download_jar(download_url)
    if download_file is not None:
        df_dependency.loc[df_dependency["download_url"] == download_url, "serializable_cnt"] = get_serializable_cnt(download_file)
        os.remove(download_file)

else:
    for i in range(0, len(download_urls) - 1):
        download_url_cur = download_urls[i]
        download_file_cur = download_jar(download_url_cur)
        if download_file_cur is None:
            continue

        download_file_next = None
        next_index = i + 1
        while next_index < len(download_urls):
            download_url_next = download_urls[next_index]
            download_file_next = download_jar(download_url_next)
            if download_file_next is not None:
                break
            next_index += 1

        if download_file_next is None:
            # only calculate serializable cnt ...
            df_dependency.loc[df_dependency["download_url"] == download_url_cur, "serializable_cnt"] = get_serializable_cnt(download_file_cur)
            os.remove(download_file_cur)
            break

        df_dependency.loc[df_dependency["download_url"] == download_url_cur, "next_version"] = versions[next_index]
        all_stats = get_all_stats(download_file_cur, download_file_next)
        if all_stats is not None:
            for key in all_stats:
                if key == "serializable_cnt":
                    df_dependency.loc[df_dependency["download_url"] == download_url_cur, "serializable_cnt"] = all_stats[key]
                else:
                    df_dependency.loc[df_dependency["download_url"] == download_url_next, key] = all_stats[key]

        os.remove(download_file_cur)

        # If this is the last file we also calculate the serializable_cnt for it
        if i == len(download_urls) - 2:
            # only calculate serializable cnt ...
            df_dependency.loc[df_dependency["download_url"] == download_url_next, "serializable_cnt"] = get_serializable_cnt(download_file_next)
            os.remove(download_file_next)
            break


if download_file_next is not None and os.path.exists(download_file_next):
    os.remove(download_file_next)

df_dependency.drop(columns=["relocation_name", "usage_cnt"], inplace=True)
print(df_dependency.loc[df_dependency["dependency_name"] == dependency].to_string())
df_dependency.to_csv(os.path.join(OUTPUT_DIR, f"{dependency}.csv"))
import os
import requests
import pandas as pd
import zipfile
import shutil
import sys

DOWNLOAD_DIR = os.path.join("..", "input", "libs")
DATASET_D = os.path.join("..", "data", "final_datasets", "gadget_providers_volatile_stealthy.csv")
DATASET_B = os.path.join("..", "data", "final_datasets", "gadget_providers_active.csv")
DATASET_A = os.path.join("..", "data", "final_datasets", "gadget_providers.csv")


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


dataset = ""
if sys.argv[1] == "A":
    dataset = DATASET_A
elif sys.argv[1] == "B":
    dataset = DATASET_B
elif sys.argv[1] == "D":
    dataset = DATASET_D
else:
    exit(1)

df = pd.read_csv(dataset)
df["parsed_date"] = pd.to_datetime(df["parsed_date"])
dependency_names = df.groupby("dependency_name").count().index

print(len(dependency_names))


for dependency in dependency_names:
    break
    df_dependency = df.loc[df["dependency_name"] == dependency]
    latest = df_dependency.loc[df_dependency['parsed_date'] == df_dependency["parsed_date"].max()]
    download_url = latest['download_url'].values[0]

    output_file_name = latest['dependency_name'].values[0] + "-" + latest['version'].values[0] + download_url[-4:]

    if os.path.exists(os.path.join(DOWNLOAD_DIR, output_file_name)):
        continue

    response = requests.get(download_url, allow_redirects=True)
    open(os.path.join(DOWNLOAD_DIR, output_file_name), 'wb').write(response.content)
    if output_file_name[-4:] == ".aar":
        aar_to_jar(os.path.join(DOWNLOAD_DIR, output_file_name), DOWNLOAD_DIR, output_file_name.replace(".aar", ".jar"))



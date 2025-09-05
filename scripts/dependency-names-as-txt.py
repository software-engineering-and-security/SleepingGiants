import os
import pandas as pd

INPUT_FILE = os.path.join('input', 'clean_versions.csv')
if os.path.split(os.getcwd())[-1] == "scripts":
    INPUT_FILE = os.path.join('..', INPUT_FILE)

df = pd.read_csv(INPUT_FILE)
dependency_names = df.groupby('dependency_name').count().index.tolist()
with open("dependency_names.txt", "w") as output_file:
    for dependency in dependency_names:
        output_file.write(dependency + "\n")
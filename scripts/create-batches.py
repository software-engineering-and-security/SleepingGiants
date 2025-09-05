import os.path

dependencies_a = open(os.path.join("..", "data", "final_datasets", "dependencies_A.txt"), "r").read().split("\n")
dependencies_b = open(os.path.join("..", "data", "final_datasets", "dependencies_B.txt"), "r").read().split("\n")
dependencies_d = open(os.path.join("..", "data", "final_datasets", "dependencies_D.txt"), "r").read().split("\n")

for dep in dependencies_d:
    print(dep)

print("-----------------")

for dep in dependencies_b:
    if dep not in dependencies_d:
        print(dep)

print("-----------------")

for dep in dependencies_a:
    if dep not in dependencies_b:
        print(dep)
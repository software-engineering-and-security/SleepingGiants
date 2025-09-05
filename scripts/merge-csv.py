import os
import pandas as pd


csv_dir = os.path.join("..", "output")
csv_files = [os.path.join(csv_dir, f) for f in os.listdir(csv_dir) if f.endswith('.csv')]

df = pd.concat((pd.read_csv(f) for i, f in enumerate(csv_files)), ignore_index=True)
df.to_csv(os.path.join("..", "merged.csv"), index=False)

## Artifact Evaluation

## Requirements

- Python 3

```
requests==2.32.3
pandas==2.2.3
beautifulsoup4==4.13.4
cfscrape==2.1.1
tqdm==2.2.3
matplotlib==3.10.1
neo4j==5.28.0
```

- Java (OpenJDK 21.0.8)
- Maven (3.9.9)
- Docker (for running Tabby and Crystallizer)
- GNU Parallel (``20240222``)

## Section 3

### Step 1

We collected all popular dependencies from mvnrepository.com using the script ``download-libs.py``. Since this uses a webscraper and can be throttled by Cloudflare, we had to resort to using ``download-cloudflare-alternative.py`` later. 
The latter script works by parsing all files in a subdirectory ``./html/``. The HTML files are the category listings ``https://mvnrepository.com/open-source/<category>?p=<pagination>``, e.g., https://mvnrepository.com/open-source/jvm-languages?p=1. One html file per pagination.

```bash
python3 download-libs.py

# Or semi-automated because of cloudflare:
mkdir html

# Save pages in html as: 1.html, 2.html, ..., n.html
# https://mvnrepository.com/open-source/jvm-languages?p=1
# https://mvnrepository.com/open-source/jvm-languages?p=2
# ...

python3 download-cloudflare-alternative.py
```

### Step 2

This creates a collection of files called ``download_<category>_<page>.csv``. For reference, our output is in ``input/single``. We then merge the output files with 

```bash
python3 merge-csv.py
```

### Step 3

This will give you an output file ``input/merged.csv`` (view our merged.csv for reference) which is initially cleaned with ``data-cleaning.ipynb``.
Result: ``input/clean_versions.csv``

### Step 4 

Now we enrich the data with the serializable class usage per dependency/version. First build the analysis JAR:

```bash
mvn clean package
mv target/serevol-1.0-jar-with-dependencies.jar scripts/jcl-serializable.jar
```

And then run the ``get_evolution.py`` script in parallel over all dependencies:

```bash
python3 dependency-names-as-txt.py
# output depedency_names.txt

parallel -j128 python3 get_evolution.py {} :::: dependency_names.txt
```

As a result there is one csv file per dependency in ``output/evolution-csv-single``. These are again merged to a single file with ``merge-csv.py``. Output file: ``data/merged.csv``.

### Step 5

Post analysis data cleaning ``scripts/data-cleaning-post-analysis.ipynb``. This results in the datasets mentioned in Section 3.3 - Key Observations:

- (A) Gadget Providers: ``data/final_datasets/gadget_providers.csv``
- (B) Active Gadget Providers: ``data/final_datasets/gadget_providers_active.csv``
- (C) Volatile Gadget Providers: ``data/final_datasets/gadget_providers_volatile.csv``
- (D) Volatile Stealthy Gadget Providers: ``data/final_datasets/gadget_providers_volatile_stealthy.csv``


## Section 4

### Step 1 - Creating the gadget-injected datasets

```bash
cd scripts

# Build gadget inject JAR
mvn clean package
mv target/GadgetInject-1.0-jar-with-dependencies.jar scripts/gadget-inject.jar


# populate the unmodified latest JAR file directory (input/libs)
python3 download-latest-lib.py C

# Run Gadget Injection for modifications (single modification only)
python3 inject.py ../input/libs trampoline

# merged dataset:
python3 inject.py ../input/libs all
mv ../output/modification_all ../output/modification_a_and_b
python3 inject.py ../output/modification_a_and_b trampoline
mv ../output/modification_c ../output/modification_all_C

# now further populate the input/libs directory for dataset B:
python3 download-latest-lib.py B

# Rerun gadget injection now only using final and trampoline modification because "ser" is not applicable
python3 inject.py ../input/libs final
python3 inject.py ../output/modification_b trampoline
mv ../output/modification_c ../output/modification_all_B

# now get the full dataset which is only applicable to the "trampoline" modification
python3 download-latest-lib.py D
python3 inject.py ../input/libs trampoline
```

Looking at Table 5, we now have the outputs:

| | (1) | (2) | (3) | all |
| ----- | ----- | ----- | ----- | ----- |
| A (533) | - | - | ``output/modification_c`` | ``output/modification_c`` |
| B (352) | - | ``output/modification_b`` | ``output/modification_c`` | ``output/modification_all_B`` |
| A (208) | ``output/modification_a`` | ``output/modification_b`` | ``output/modification_c`` | ``output/modification_all_C`` |

For reference, these outputs refer to the following artifacts:

- *output/modification_a* : *stealthy_serializable_mod.tar.gz*
- *output/modification_b* : *stealthy_privatefinal_mod.tar.gz*
- *output/modification_c* : *stealthy_trampoline_mod.tar.gz*
- *output/modification_all_C* : *stealthy_all_mod.tar.gz*
- *output/modification_all_B* : *stealthy_all_mod_B.tar.gz*

### Step 2 - Running Gadget Chain Detectors

By running the gadget chain detectors over the unmodified dependencies and the modified datasets we receive the results in ``output/results/<androChain|crystallizer|tabby>``, artifacts *androChain_results.tar.gz*, *crystallizer_results.tar.gz*, *tabby_results_tar.gz*. 



#### AndroChain

```bash
# It uses the ../input/libs/ dir for comparison as default
parallel -j128 python3 run-androchain.py {} <modification> :::: <batch_file>

# e.g.:
parallel -j128 python3 run-androchain.py {} a :::: ../data/final_datasets/dependencies_D.txt

```

#### Crystallizer
```bash
# setup containers to receive batches and teardown -> creates directories crystallizer/0 , crystallizer/1, etc. 
python3 run-crystallizer.py start <container_cnt>
python3 run-crystallizer.py kill <container_cnt>

# run a batch of dependencies on Crystallizer containers (starting at container offset)
python3 run-crystallizer.py run <batch_file>
python3 run-crystallizer.py run <batch_file> <offset> <dependency_dir>

# we can use "peek" to check whether the sinkID has been fruitful
python3 run-crystallizer.py peek <container_cnt>

# save results
python3 run-crystallizer.py store <container_cnt> <save_dir> <offset>
```

#### Tabby

```bash
# if necessary build the Tabby image
cd scripts/tabby_docker
docker build docker build -t tabby_neo4j .

# Start/Kill neo4j databases:
python3 run-tabby.py neo4j start <container_cnt>
python3 run-tabby.py neo4j kill <container_cnt>

# Batch execution (make sure the batch size matches container_cnt)
parallel python3 run-tabby.py run {1} {2} <target> :::: <batch_file> :::+ {0..<container_cnt-1>}
# e.g., running a batch_D_1.txt with 50 jar files defined inside
parallel python3 run-tabby.py run {1} {2} a :::: batch_D_1.txt :::+ {0..49}
```

### Step 3 - Filtering for manual analysis

```bash
cd scripts
python3 filter-gc.py filter
```

This leaves us with the artifact *filtered.tar* for manual analysis.





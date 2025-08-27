## Artifact Evaluation

### Requirements

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

### Section 3

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

This creates a collection of files called ``download_<category>_<page>.csv``. For reference, our output is in ``input/single``. We then merge the output files with 

```bash
python3 merge-csv.py

```

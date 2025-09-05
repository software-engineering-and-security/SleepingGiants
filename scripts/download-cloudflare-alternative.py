import os
import requests
from bs4 import BeautifulSoup
from datetime import datetime
from tqdm import tqdm
import cfscrape

MAVEN_POPULAR_LISTING = "https://mvnrepository.com/popular"
MAVEN_CATEGORY_LISTING = "https://mvnrepository.com/open-source"
MAVEN_BASE_URL = "https://mvnrepository.com"

ALPHA_TAGS = ["Alpha", "Beta", "ALPHA", "BETA", "alpha", "beta", "incubating", "public_draft",
              "milestone", "preview", "Preview", "PREVIEW", "SNAPSHOT", "Snapshot", "snapshot", "rc", "RC", "SEC", "sec", "Sec", "patch", "test"]


REPOSITORY_URLS = [
    "https://repo1.maven.org/maven2",
    "https://repo.clojars.org",
    "https://repo.akka.io/maven",
    "https://maven.google.com",
    "https://maven.artifacts.atlassian.com",
    "https://maven.wso2.org/nexus/content/repositories/releases",
    "https://nexus.bedatadriven.com/content/groups/public",
    "https://repository.mulesoft.org/nexus/content/repositories/public",
    "https://repo.jenkins-ci.org/releases",
    "https://nexus.senbox.net/nexus/content/repositories/releases",
    "https://open.artefacts.tax.service.gov.uk/maven2"
]

EXCLUDE_CATEGORIES = ["testing-frameworks", "mocking", "build-tools", "gradle-plugins"]


def paginate_popular_listing(page):
    return f"{MAVEN_POPULAR_LISTING}?p={page}"


def paginate_category_listing(page):
    return f"{MAVEN_CATEGORY_LISTING}?p={page}"


def paginate_category(category, page):
    print(f"{MAVEN_CATEGORY_LISTING}/{category}?sort=popular&p={page}")
    return f"{MAVEN_CATEGORY_LISTING}/{category}?sort=popular&p={page}"


def get_all_from_page_listing(html_input, output_file):
    artifact_soup = BeautifulSoup(open(html_input), 'html.parser')
    dependency_url = artifact_soup.find("h2", {'class': ['im-title']}).find("a").attrs['href']
    dependency_name = dependency_url.split("/")[-1]
    package_name = dependency_url.split("/")[-2]
    usage_cnt = 0

    for row in artifact_soup.find("table", attrs={'class': ['grid']}).find_all("tr"):
        if row.find("th").text == "Used By":
            usage_cnt = row.find("td").find("a").find("b").text.replace("artifacts", "").strip().replace(",", "")

    versions_table = artifact_soup.find("table", {'class': ['versions']})

    for row in versions_table.find_all("tr"):
        version_btn = row.find_next("a", attrs={"class": ["vbtn"]})
        version = version_btn.text

        # filter alpha releases:
        skip_alpha = False
        for alpha_tag in ALPHA_TAGS:
            if alpha_tag in version:
                skip_alpha = True
                break
        if skip_alpha:
            continue

        # in cases like Kafka there are different subversions of Kafka. We can see this from the version btn link
        alternate_dep_name = version_btn.attrs['href'].split("/")[-2]

        date = row.find_next("td", attrs={"class": ["date"]}).text
        parsed_date = datetime.strptime(date, "%b %d, %Y").strftime("%Y-%m-%d")

        download_url_base = f"{package_name.replace('.', '/')}/{alternate_dep_name}/{version}/{alternate_dep_name}-{version}"
        download_url = ""
        download_success = False

        for repository_url in REPOSITORY_URLS:
            download_url = f"{repository_url}/{download_url_base}.jar"
            download_response_check = requests.head(download_url,allow_redirects=True)
            if download_response_check.status_code < 400:
                download_success = True
                break
            download_url = f"{repository_url}/{download_url_base}.aar"
            download_response_check = requests.head(download_url, allow_redirects=True)
            if download_response_check.status_code < 400:
                download_success = True
                break

        if download_success:
            output_file.write(f"{package_name}.{dependency_name};{version};{parsed_date};{usage_cnt};{download_url};\n")
            print(f"{package_name}.{dependency_name};{version};{parsed_date};{usage_cnt};{download_url};")


category = "swing-libraries"
file_name = f"download_{category}_1.csv"
output_file = open(file_name, "w")
output_file.write("dependency_name;version;parsed_date;usage_cnt;download_url;relocation_name\n")

index = 1
for file in os.listdir("html"):
    if os.path.exists(os.path.join("html", f"{index}.html")):
        get_all_from_page_listing(os.path.join("html", f"{index}.html"), output_file)
    index += 1

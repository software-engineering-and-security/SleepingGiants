import sys
import requests
from bs4 import BeautifulSoup
import cloudscraper
from datetime import datetime
from tqdm import tqdm
import cfscrape

MAVEN_POPULAR_LISTING = "https://mvnrepository.com/popular"
MAVEN_CATEGORY_LISTING = "https://mvnrepository.com/open-source"
MAVEN_BASE_URL = "https://mvnrepository.com"

ALPHA_TAGS = ["Alpha", "Beta", "ALPHA", "BETA", "alpha", "beta", "incubating", "public_draft",
              "milestone", "preview", "Preview", "PREVIEW", "SNAPSHOT", "Snapshot", "snapshot", "rc", "RC", "SEC", "sec", "Sec"]


REPOSITORY_URLS = [
    "https://repo1.maven.org/maven2",
    "https://maven.google.com",
    "https://maven.artifacts.atlassian.com",
    "https://maven.wso2.org/nexus/content/repositories/releases",
    "https://nexus.bedatadriven.com/content/groups/public",
    "https://repository.mulesoft.org/nexus/content/repositories/public",
    "https://repo.jenkins-ci.org/releases",
    "https://nexus.senbox.net/nexus/content/repositories/releases"
]

EXCLUDE_CATEGORIES = ["testing-frameworks", "mocking", "build-tools", "gradle-plugins"]


def paginate_popular_listing(page):
    return f"{MAVEN_POPULAR_LISTING}?p={page}"


def paginate_category_listing(page):
    return f"{MAVEN_CATEGORY_LISTING}?p={page}"


def paginate_category(category, page):
    print(f"{MAVEN_CATEGORY_LISTING}/{category}?sort=popular&p={page}")
    return f"{MAVEN_CATEGORY_LISTING}/{category}?sort=popular&p={page}"


def get_all_from_page_listing(url, output_file, usage_thresh_upper=100000000, usage_thresh_lower=100):

    #scraper = cloudscraper.create_scraper(browser={"browser" : "chrome", "platform" : "windows"})
    scraper = cfscrape.create_scraper()
    response = scraper.get(url)
    print(response.status_code)
    soup = BeautifulSoup(response.content, 'html.parser')

    for dependency in tqdm(soup.find_all(class_='im-title')):
        dependency_sub_url = dependency.find_next("a").attrs['href']
        response = scraper.get(f"{MAVEN_BASE_URL}{dependency_sub_url}")
        artifact_soup = BeautifulSoup(response.content, 'html.parser')

        dependency_name = dependency_sub_url.split("/")[-1]
        package_name = dependency_sub_url.split("/")[-2]

        usage_cnt = 0
        usage_cnt_tag = dependency.find_next("a", attrs={"class": ["im-usage"]})
        if usage_cnt_tag is not None:
            usage_cnt = usage_cnt_tag.find("b").text.replace(",", "")

        if int(usage_cnt) > usage_thresh_upper or int(usage_cnt) < usage_thresh_lower:
            continue

        # Handle package relocation
        relocation = dependency.parent.find_next_sibling("div", attrs={"class": ["im-description"]}).find("div", attrs={"class": ["im-relocation"]})
        relocation_name = ""
        if relocation is not None:
            relocation_package_name=relocation.find_all("a")[0].text
            relocation_dependency_name=relocation.find_all("a")[1].text
            relocation_name = relocation_package_name + "." + relocation_dependency_name

        # Filter testing/mocking categories
        is_exclude_category = False

        for link in artifact_soup.find_all("th"):
            if link.text == "Categories":
                if link.find_next("a").attrs["href"].split("/")[-1] in EXCLUDE_CATEGORIES:
                    is_exclude_category=True
                break

        if is_exclude_category:
            continue

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
                output_file.write(f"{package_name}.{dependency_name};{version};{parsed_date};{usage_cnt};{download_url};{relocation_name}\n")


def get_most_popular(page):
    output_file = open(f"download_{page}.csv", "w")
    output_file.write("dependency_name;version;parsed_date;usage_cnt;download_url;relocation_name\n")
    get_all_from_page_listing(paginate_popular_listing(page), output_file)


def get_by_category(category, page):
    output_file = open(f"download_{category}_{page}.csv", "w")
    output_file.write("dependency_name;version;parsed_date;usage_cnt;download_url;relocation_name\n")
    get_all_from_page_listing(paginate_category(category, page), output_file, 2745, 100)


def get_categories():
    categories = []

    for page in range(1,16):
        scraper = cloudscraper.create_scraper(browser={"browser" : "chrome", "platform" : "windows"})
        response = scraper.get(paginate_category_listing(page))
        soup = BeautifulSoup(response.content, 'html.parser')

        for heading in soup.find_all("h4"):
            category_link = heading.find_next("a")
            if category_link is not None and "open-source" in category_link.attrs["href"]:
                category_name = category_link.attrs["href"].split("/")[-1]
                if not category_name in EXCLUDE_CATEGORIES:
                    categories.append(category_name)

    return categories


for category in get_categories():
    for page in range(1,16):
        get_by_category(category,page)






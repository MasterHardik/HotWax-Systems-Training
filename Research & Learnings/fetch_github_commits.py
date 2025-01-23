import os
import requests
import json

# Define the repository details
repo_owner = "MasterHardik"
repo_name = "[repo-name]"
base_url = "https://api.github.com"

# Create a folder to store the data
output_folder = 'commit_data'
os.makedirs(output_folder, exist_ok=True)

# Function to get all branches in the repository
def get_branches(owner, repo):
    url = f"{base_url}/repos/{owner}/{repo}/branches"
    response = requests.get(url)
    if response.status_code == 200:
        branches = [branch['name'] for branch in response.json()]
        return branches
    else:
        print(f"Error fetching branches: {response.status_code}")
        return []

# Function to get all commit authors for a given branch
def get_commit_authors(owner, repo, branch):
    url = f"{base_url}/repos/{owner}/{repo}/commits?sha={branch}"
    authors = set()
    
    while url:
        response = requests.get(url)
        if response.status_code == 200:
            commits = response.json()
            for commit in commits:
                authors.add(commit['commit']['author']['name'])  # Add commit author's name
            # Check if there is a next page
            if 'Link' in response.headers:
                # Extract 'next' link from the pagination header
                links = response.headers['Link'].split(',')
                next_page_url = None
                for link in links:
                    if 'rel="next"' in link:
                        next_page_url = link.split(';')[0][1:-1]
                url = next_page_url if next_page_url else None
            else:
                break
        else:
            print(f"Error fetching commits for branch {branch}: {response.status_code}")
            break
    return authors

# Function to get all commits made by a specific user on a given branch
def get_commits_by_user(owner, repo, branch, user):
    url = f"{base_url}/repos/{owner}/{repo}/commits?sha={branch}&author={user}"
    commits = []
    
    while url:
        response = requests.get(url)
        if response.status_code == 200:
            commits.extend(response.json())  # Add commit data to list
            # Check for pagination
            if 'Link' in response.headers:
                links = response.headers['Link'].split(',')
                next_page_url = None
                for link in links:
                    if 'rel="next"' in link:
                        next_page_url = link.split(';')[0][1:-1]
                url = next_page_url if next_page_url else None
            else:
                break
        else:
            print(f"Error fetching commits for user {user} on branch {branch}: {response.status_code}")
            break
    return commits

# Main logic to iterate through branches and users
def main():
    branches = get_branches(repo_owner, repo_name)
    
    # Iterate through each branch
    for branch in branches:
        print(f"Processing branch: {branch}")
        
        # Get all unique authors for the current branch
        authors = get_commit_authors(repo_owner, repo_name, branch)
        print(f"Found {len(authors)} authors for branch: {branch}")
        
        # Iterate through each user and get their commits on this branch
        for author in authors:
            print(f"Fetching commits for user: {author} on branch {branch}")
            commits = get_commits_by_user(repo_owner, repo_name, branch, author)
            
            # Store the commits data in a file named `branch_user.json`
            file_name = f"{branch}_{author.replace('/', '_')}.json"  # Avoid slashes in filename
            file_path = os.path.join(output_folder, file_name)
            
            # Write the commits data to a JSON file
            with open(file_path, 'w', encoding='utf-8') as json_file:
                json.dump(commits, json_file, ensure_ascii=False, indent=4)
            
            print(f"Saved commits for {author} on {branch} to {file_path}")

if __name__ == "__main__":
    main()

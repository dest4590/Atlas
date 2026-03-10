try:
    import bcrypt
except ImportError:
    print("bcrypt library not found. Please install it using pip install bcrypt")
    exit(1)

import getpass

OUTPUT_FILE = "prometheus-htpasswd.txt"

def generate_htpasswd(username: str, password: str) -> str:
    salt = bcrypt.gensalt()
    hashed = bcrypt.hashpw(password.encode(), salt)
    return f"{username}:{hashed.decode()}"

def main():
    print("htpasswd generator")

    username = input("username: ")
    password = getpass.getpass("password: ")

    entry = generate_htpasswd(username, password)

    with open(OUTPUT_FILE, "w") as f:
        f.write(entry + "\n")

    print(f"here we go: {OUTPUT_FILE}")

if __name__ == "__main__":
    main()
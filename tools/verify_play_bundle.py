"""Verify a release bundle against the previously used public upload certificate."""
import argparse
from pathlib import Path
import re
import subprocess

EXPECTED = "76110773e7fa518f553f79a3b9a48aac1d5c5b45f3913d58467b7de01fc20c6a"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("bundle", type=Path)
    parser.add_argument("--jdk-bin", type=Path, required=True)
    args = parser.parse_args()
    suffix = ".exe" if (args.jdk_bin / "keytool.exe").exists() else ""
    certificate = subprocess.run([str(args.jdk_bin / ("keytool" + suffix)), "-J-Duser.language=en", "-printcert", "-jarfile", str(args.bundle)], capture_output=True, text=True)
    fingerprints = re.findall(r"SHA256:\s*([A-Fa-f0-9:]+)", certificate.stdout)
    if not fingerprints or {value.replace(":", "").lower() for value in fingerprints} != {EXPECTED}:
        raise SystemExit("Bundle unsigned or signed with a different upload certificate")
    verification = subprocess.run([str(args.jdk_bin / ("jarsigner" + suffix)), "-J-Duser.language=en", "-verify", str(args.bundle)], capture_output=True, text=True)
    if verification.returncode or "jar verified" not in verification.stdout.lower():
        raise SystemExit("Bundle signature verification failed")
    print("PASS — bundle integrity verified with the same upload certificate as bundle 54")


if __name__ == "__main__":
    main()

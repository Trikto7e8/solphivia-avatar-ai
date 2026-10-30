#!/usr/bin/env python3
"""Check ELF PT_LOAD alignment in a release AAB (not APK ZIP alignment)."""
import struct
import sys
import zipfile

def load_alignments(data):
    if data[:4] != b"\x7fELF" or data[4] not in (1, 2) or data[5] not in (1, 2):
        raise ValueError("unsupported ELF header")
    endian = "<" if data[5] == 1 else ">"
    if data[4] == 2:
        phoff = struct.unpack_from(endian + "Q", data, 32)[0]
        phentsize, phnum = struct.unpack_from(endian + "HH", data, 54)
        fmt, minimum = "IIQQQQQQ", 56
    else:
        phoff = struct.unpack_from(endian + "I", data, 28)[0]
        phentsize, phnum = struct.unpack_from(endian + "HH", data, 42)
        fmt, minimum = "IIIIIIII", 32
    if phentsize < minimum or phoff + phnum * phentsize > len(data):
        raise ValueError("invalid program-header table")
    result = []
    for i in range(phnum):
        header = struct.unpack_from(endian + fmt, data, phoff + i * phentsize)
        if header[0] == 1:
            alignment = header[-1]
            offset, vaddr = (header[2], header[3]) if data[4] == 2 else (header[1], header[2])
            result.append(alignment if offset % 16384 == vaddr % 16384 else 0)
    if not result:
        raise ValueError("ELF has no loadable segments")
    return result

def check(path):
    failures, checked = [], 0
    with zipfile.ZipFile(path) as archive:
        libs = sorted(n for n in archive.namelist()
                      if n.endswith(".so") and
                      ("/lib/arm64-v8a/" in n or "/lib/x86_64/" in n))
        for name in libs:
            checked += 1
            try:
                values = load_alignments(archive.read(name))
                if any(a < 16384 or a & (a - 1) for a in values):
                    failures.append(name + ": incompatible PT_LOAD alignment")
                else:
                    print("PASS", name)
            except (ValueError, struct.error) as exc:
                failures.append(name + ": " + str(exc))
    for failure in failures:
        print("FAIL", failure, file=sys.stderr)
    print(f"Checked {checked} 64-bit native libraries; failures: {len(failures)}.")
    if not checked:
        print("No 64-bit native libraries found; verify expected ABIs.", file=sys.stderr)
        return 1
    return 1 if failures else 0

if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("Usage: check_native_alignment.py app-release.aab")
    sys.exit(check(sys.argv[1]))

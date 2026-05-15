# -*- mode: python ; coding: utf-8 -*-

import sys
import os
from pathlib import Path

block_cipher = None

# Get the base directory at build time
BASE_DIR = Path(SPECPATH).parent.absolute()

a = Analysis(
    [str(BASE_DIR / 'launcher.py')],
    pathex=[str(BASE_DIR)],
    binaries=[],
    datas=[
        # Include all service directories
    ],
    hiddenimports=[
        'pkg_resources',
        'subprocess',
        'threading',
    ],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[],
    win_no_prefer_redirects=False,
    win_private_assemblies=False,
    cipher=block_cipher,
    noarchive=False,
)

pyz = PYZ(a.pure, a.zipped_data, cipher=block_cipher)

exe = EXE(
    pyz,
    a.scripts,
    [],
    exclude_binaries=True,
    name='JiraPlatformLauncher',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    console=True,
    disable_windowed_traceback=False,
    argv_emulation=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
    icon=None,
)

coll = COLLECT(
    exe,
    a.binaries,
    a.zipfiles,
    a.datas,
    strip=False,
    upx=True,
    upx_exclude=[],
    name='JiraPlatformLauncher',
)

# Output to dist/ folder next to launcher.py
dist_dir = BASE_DIR / 'dist'
exe_location = dist_dir / 'JiraPlatformLauncher.exe'

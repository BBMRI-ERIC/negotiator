#!/usr/bin/env python3
"""
Create minimal valid DOC and DOCX files for testing.
"""

import zipfile
import struct

def create_docx():
    """Create a minimal valid DOCX file."""
    
    content_types = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>'''

    main_rels = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>'''

    word_rels = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
</Relationships>'''

    document_xml = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:body>
<w:p>
<w:r>
<w:t>Test Document for PDF Conversion</w:t>
</w:r>
</w:p>
<w:p>
<w:r>
<w:t>This is a valid DOCX document that can be converted to PDF.</w:t>
</w:r>
</w:p>
<w:p>
<w:r>
<w:t>Line 3: Additional content for testing.</w:t>
</w:r>
</w:p>
</w:body>
</w:document>'''

    with zipfile.ZipFile('test.docx', 'w', zipfile.ZIP_DEFLATED) as docx:
        docx.writestr('[Content_Types].xml', content_types)
        docx.writestr('_rels/.rels', main_rels)
        docx.writestr('word/_rels/document.xml.rels', word_rels)
        docx.writestr('word/document.xml', document_xml)
    
    print("Created test.docx")

def create_doc():
    """Create a minimal valid DOC file."""
    # DOC file header (OLE2 format)
    header = bytearray(512)
    
    # OLE2 signature
    header[0:8] = b'\xd0\xcf\x11\xe0\xa1\xb1\x1a\xe1'
    
    # Minor version
    header[24:26] = struct.pack('<H', 0x003E)
    
    # Major version  
    header[26:28] = struct.pack('<H', 0x003E)
    
    # Byte order
    header[28:30] = struct.pack('<H', 0xFFFE)
    
    # Sector size (512 bytes = 2^9)
    header[30:32] = struct.pack('<H', 0x0009)
    
    # Mini sector size (64 bytes = 2^6)
    header[32:34] = struct.pack('<H', 0x0006)
    
    # Number of directory sectors
    header[44:48] = struct.pack('<L', 0x00000001)
    
    # Number of FAT sectors
    header[48:52] = struct.pack('<L', 0x00000001)
    
    # Directory first sector
    header[52:56] = struct.pack('<L', 0x00000001)
    
    # Transaction signature
    header[56:60] = struct.pack('<L', 0x00000000)
    
    # Mini stream cutoff
    header[60:64] = struct.pack('<L', 0x00001000)
    
    # First mini FAT sector
    header[64:68] = struct.pack('<L', 0xFFFFFFFE)
    
    # Number of mini FAT sectors
    header[68:72] = struct.pack('<L', 0x00000000)
    
    # First difat sector
    header[72:76] = struct.pack('<L', 0xFFFFFFFE)
    
    # Number of difat sectors
    header[76:80] = struct.pack('<L', 0x00000000)
    
    # First 109 DIFAT entries
    header[76:80] = struct.pack('<L', 0x00000000)  # First FAT sector at sector 0
    for i in range(1, 109):
        header[76 + i*4:80 + i*4] = struct.pack('<L', 0xFFFFFFFE)
    
    # Create a simple DOC file with minimal content
    doc_data = header + b'\x00' * (1024 - len(header))  # Pad to 1024 bytes
    
    with open('test.doc', 'wb') as f:
        f.write(doc_data)
    
    print("Created test.doc")

if __name__ == "__main__":
    create_docx()
    create_doc()
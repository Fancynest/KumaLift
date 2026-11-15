import os
import pypdf

PDF_PATH = "documentation/KumaLift_User_Manual.pdf"

def qa_pdf():
    print(f"Inspecting PDF: {PDF_PATH}")
    assert os.path.exists(PDF_PATH), "PDF file does not exist!"
    size = os.path.getsize(PDF_PATH)
    print(f"File size: {size:,} bytes ({size / 1024 / 1024:.2f} MB)")
    
    reader = pypdf.PdfReader(PDF_PATH)
    total_pages = len(reader.pages)
    print(f"Total pages: {total_pages}")
    assert total_pages == 20, f"Expected 20 pages, got {total_pages}"
    
    # Check text extractability across all pages
    print("\n--- Verifying Searchable Text Across Pages ---")
    for i, page in enumerate(reader.pages):
        text = page.extract_text()
        first_line = text.strip().split("\n")[0] if text else "EMPTY"
        word_count = len(text.split())
        print(f"Page {i+1:2d}: {word_count:4d} words | Header/Start: {first_line[:50]}...")
        assert word_count > 20, f"Page {i+1} has suspiciously low text content ({word_count} words)"
    
    # Check metadata
    print("\n--- Document Metadata ---")
    meta = reader.metadata
    for k, v in meta.items():
        print(f"  {k}: {v}")
    assert meta.title, "Metadata title missing!"
    
    # Check outline / bookmarks
    print("\n--- Document Outline / Bookmarks ---")
    outline = reader.outline
    def print_outline(items, indent=0):
        for item in items:
            if isinstance(item, list):
                print_outline(item, indent + 2)
            else:
                title = item.title if hasattr(item, 'title') else str(item)
                print(" " * indent + f"• {title}")
    print_outline(outline)
    assert len(outline) > 0, "No outline bookmarks found!"
    
    print("\nQuality Assurance Check: 100% PASSED!")

if __name__ == "__main__":
    qa_pdf()

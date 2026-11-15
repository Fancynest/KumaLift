import pymupdf
import os

def render_pdf_pages(pdf_path, output_dir, dpi=150):
    os.makedirs(output_dir, exist_ok=True)
    doc = pymupdf.open(pdf_path)
    print(f"Rendering {pdf_path} ({len(doc)} pages) to {output_dir}...")
    
    scale = dpi / 72.0
    mat = pymupdf.Matrix(scale, scale)
    
    rendered_files = []
    for i, page in enumerate(doc):
        pix = page.get_pixmap(matrix=mat, alpha=False)
        out_path = os.path.join(output_dir, f"page_{i+1:02d}.png")
        pix.save(out_path)
        rendered_files.append((out_path, pix.width, pix.height))
        print(f"  Page {i+1:02d} rendered -> {out_path} ({pix.width}x{pix.height})")
        
    return rendered_files

def run_qa():
    print("=== Step 1: Rendering Indonesian PDF Pages ===")
    id_renders = render_pdf_pages("documentation/KumaLift_User_Guide_ID.pdf", "documentation/qa_renders/id", dpi=150)
    
    print("\n=== Step 2: Rendering English PDF Pages ===")
    en_renders = render_pdf_pages("documentation/KumaLift_User_Guide_EN.pdf", "documentation/qa_renders/en", dpi=150)
    
    print(f"\nCompleted: {len(id_renders)} ID pages, {len(en_renders)} EN pages rendered.")

if __name__ == "__main__":
    run_qa()

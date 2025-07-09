import sys
from PIL import Image

try:
    # Open the original image
    original_image_path = "app/src/main/res/drawable/tulsi.png"
    output_image_path = "temp/tulsi_fixed.png"
    
    # Open and convert the image
    img = Image.open(original_image_path)
    
    # Convert to RGB if it has alpha channel
    if img.mode == 'RGBA':
        img = img.convert('RGBA')
    
    # Resize if needed (optional)
    # img = img.resize((512, 512), Image.LANCZOS)
    
    # Save with optimized settings
    img.save(output_image_path, 'PNG', optimize=True)
    
    print(f"Image successfully optimized and saved to {output_image_path}")
    
except Exception as e:
    print(f"Error processing image: {e}")
    sys.exit(1)

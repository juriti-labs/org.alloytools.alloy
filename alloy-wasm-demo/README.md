# Alloy WASM Demo

A proof-of-concept web application demonstrating Alloy running in the browser.

## Quick Start

### Option 1: Using Python (Easiest)

```bash
cd alloy-wasm-demo
python3 -m http.server 8000
```

Then open http://localhost:8000 in your browser.

### Option 2: Using Node.js

```bash
cd alloy-wasm-demo
npx http-server -p 8000
```

Then open http://localhost:8000 in your browser.

### Option 3: Using PHP

```bash
cd alloy-wasm-demo
php -S localhost:8000
```

Then open http://localhost:8000 in your browser.

## What This Demo Shows

This is a **proof of concept** demonstrating:
- A clean, modern web interface for writing Alloy models
- Example Alloy specifications (graph, family tree, file system, etc.)
- Interactive model execution simulation
- Real-time output display

## Current Implementation

This demo currently simulates Alloy execution for demonstration purposes. The full implementation would use **CheerpJ** to run the actual Alloy JAR file in the browser via WebAssembly.

## Features

- 📝 **Code Editor**: Write Alloy models with syntax highlighting
- 📚 **Examples**: Pre-loaded example models to get started quickly
- ▶️ **Execute**: Run models and see results
- 🎨 **Modern UI**: Clean, responsive interface
- ⌨️ **Keyboard Shortcuts**: Ctrl+Enter to run models

## Next Steps for Production

To make this a fully functional WASM build:

1. **Integrate CheerpJ**: Use CheerpJ to compile the Alloy JAR to WebAssembly
2. **Add Real Execution**: Connect the UI to the actual Alloy engine
3. **Visualization**: Add graphical visualization of instances
4. **File Management**: Support loading/saving .als files
5. **Advanced Features**: Add solver selection, scope configuration, etc.

## Technology Stack

- Pure HTML/CSS/JavaScript (no build step required)
- Designed for CheerpJ integration
- Mobile-responsive design
- Works offline (once loaded)

## Browser Compatibility

- Chrome/Edge: ✅ Full support
- Firefox: ✅ Full support  
- Safari: ✅ Full support
- Mobile browsers: ✅ Responsive design

## License

Same as parent project (MIT)

package black.alias.diadem;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;

import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class JSInit {
	
	private AWTGLCanvas canvas;
	private JSContext jsContext;
	private black.alias.diadem.Loaders.ScriptManager scriptManager;
	
	private Settings settings;
	private volatile boolean running = false;
	private Thread renderThread;
	private long targetFrameNanos = 0L; // when vsync=true, based on display refresh rate
	
	public static void main(String[] args) {
		new JSInit().run();
	}
	
	public void run() {
		// Initialize
		init();
	}
	
	private void init() {
		// Load settings first
		settings = Settings.load();

		JFrame frame = new JFrame(settings.getWindowTitle());
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.setSize(settings.getResolutionWidth(), settings.getResolutionHeight());
		frame.setExtendedState(settings.isFullscreen() ? JFrame.MAXIMIZED_BOTH : JFrame.NORMAL);

		// Create GLData
		GLData data = new GLData();
		data.profile = GLData.Profile.CORE;
		data.samples = 4;
		
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("mac")) {
			data.majorVersion = 3;
			data.minorVersion = 3;
			data.forwardCompatible = true;
		} else {
			data.majorVersion = 4;
			data.minorVersion = 3;
		}

		// Create an AWTGLCanvas and wire the render lifecycle
		canvas = new AWTGLCanvas(data) {
			@Override
			public void initGL() {
				// Make the context current and create capabilities
				GL.createCapabilities();
				// Initialize JS runtime and engine once GL is ready
				initJSContext();
			}

			@Override
			public void paintGL() {
				// Drive JS-side callbacks and rendering
				if (jsContext != null) {
					try {
						// Keep viewport in sync with canvas dimensions
						GL11.glViewport(0, 0, getWidth(), getHeight());
						jsContext.executeScript("runCallbacks()");
					} catch (Exception e) {
						System.err.println("Error in runCallbacks: " + e.getMessage());
						e.printStackTrace();
					}
				}
				// Present the frame
				swapBuffers();
			}
		};

		frame.add(canvas, BorderLayout.CENTER);

		// Show window
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		// Compute a pacing interval when vsync is enabled using the display's refresh rate
		if (settings.isVsync()) {
			try {
				GraphicsDevice device = frame.getGraphicsConfiguration().getDevice();
				DisplayMode dm = device.getDisplayMode();
				int rr = dm.getRefreshRate();
				if (rr <= 0) rr = 60; // fallback
				targetFrameNanos = (long)(1_000_000_000L / (double)rr);
			} catch (Throwable t) {
				targetFrameNanos = 16_666_667L; // ~60 Hz fallback
			}
		} else {
			targetFrameNanos = 0L; // unlocked
		}

		// Start render thread: if vsync=true, pace to refresh rate; else run as fast as possible.
		running = true;
		renderThread = new Thread(() -> {
			// Give the canvas a moment to initialize
			while (running && (canvas == null || !canvas.isDisplayable())) {
				try { Thread.sleep(1); } catch (InterruptedException ignored) { return; }
			}
			long next = System.nanoTime();
			while (running && canvas != null && canvas.isDisplayable()) {
				canvas.render();
				if (settings.isVsync() && targetFrameNanos > 0) {
					next += targetFrameNanos;
					long sleepNanos = next - System.nanoTime();
					if (sleepNanos > 0) {
						try {
							long ms = sleepNanos / 1_000_000L;
							int ns = (int)(sleepNanos % 1_000_000L);
							if (ms > 0) Thread.sleep(ms, ns);
							else if (ns > 0) Thread.sleep(0, ns);
						} catch (InterruptedException ie) {
							Thread.currentThread().interrupt();
							break;
						}
					} else {
						// If we fell behind, reset baseline
						next = System.nanoTime();
					}
				} else {
					// Unlocked: tiny spin/yield to keep UI responsive
					Thread.onSpinWait();
				}
			}
		}, "RenderThread");
		renderThread.setDaemon(true);
		renderThread.start();

		// Clean shutdown on window close
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) { shutdown(); }
			@Override
			public void windowClosing(WindowEvent e) { shutdown(); }
		});
	}
	
	private void initJSContext() {
		jsContext = new JSContext();
		scriptManager = new black.alias.diadem.Loaders.ScriptManager(jsContext);

		try {
			jsContext.executeScriptFile("/polyfills.js"); 
			jsContext.executeModule("import * as THREE from 'three'; globalThis.THREE = THREE;");
			jsContext.setupModelLoader();
			jsContext.setupTextureLoader();
			jsContext.executeScriptFile("/extensions.js");
			scriptManager.loadMainScript();
		} catch (Exception e) {
			System.err.println("Failed to initialize JavaScript context: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void shutdown() {
		running = false;
		if (renderThread != null) {
			try { renderThread.join(500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
			renderThread = null;
		}
		if (jsContext != null) {
			try {
				jsContext.executeScript("if (globalThis.mainEntity) globalThis.mainEntity.stop();");
			} catch (Exception e) {
				System.err.println("Error stopping Main entity: " + e.getMessage());
			}
			try {
				jsContext.close();
			} catch (Exception ignore) {}
			jsContext = null;
		}
		// Exit process after cleanup to match previous behavior
		System.exit(0);
	}
}

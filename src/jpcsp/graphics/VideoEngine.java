/*
This file is part of jpcsp.

Jpcsp is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Jpcsp is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */
package jpcsp.graphics;

import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_LIST_CANCEL_DONE;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_LIST_DONE;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_LIST_DRAWING;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_LIST_END_REACHED;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_LIST_STALL_REACHED;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_BONE0;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_BONE1;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_BONE2;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_BONE3;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_BONE4;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_BONE5;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_BONE6;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_BONE7;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_PROJECTION;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_TEXGEN;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_VIEW;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_MATRIX_WORLD;
import static jpcsp.graphics.GeCommands.*;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.State;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.PspGeList;
import jpcsp.HLE.modules.sceDisplay;
import jpcsp.HLE.modules.sceGe_user;
import jpcsp.graphics.GeContext.EnableDisableFlag;
import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.graphics.RE.buffer.IREBufferManager;
import jpcsp.graphics.capture.CaptureManager;
import jpcsp.graphics.textures.GETexture;
import jpcsp.graphics.textures.GETextureManager;
import jpcsp.graphics.textures.Texture;
import jpcsp.graphics.textures.TextureCache;
import jpcsp.hardware.Screen;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.settings.Settings;
import jpcsp.util.CpuDurationStatistics;
import jpcsp.util.DurationStatistics;
import jpcsp.util.Utilities;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

//
// Ideas for Optimization:
// - compile GE lists (or part of it) into OpenGL display list (glNewList/glCallList).
//   For example, immutable subroutines called using CALL could be compiled into a display list.
//   A first run of the game using a profiler option could be used to detect which parts
//   are immutable. This information could be stored in a file for subsequent runs and
//   used as hints for the next runs.
// - Unswizzle textures in shader (is this possible?)
//
public class VideoEngine {
    public static final int NUM_LIGHTS = 4;
    public static final int SIZEOF_FLOAT = IRenderingEngine.sizeOfType[IRenderingEngine.RE_FLOAT];
    public final static String[] psm_names = new String[]{
        "PSM_5650",
        "PSM_5551",
        "PSM_4444",
        "PSM_8888",
        "PSM_4BIT_INDEXED",
        "PSM_8BIT_INDEXED",
        "PSM_16BIT_INDEXED",
        "PSM_32BIT_INDEXED",
        "PSM_DXT1",
        "PSM_DXT3",
        "PSM_DXT5"
    };
    public final static String[] logical_ops_names = new String[]{
        "LOP_CLEAR",
        "LOP_AND",
        "LOP_REVERSE_AND",
        "LOP_COPY",
        "LOP_INVERTED_AND",
        "LOP_NO_OPERATION",
        "LOP_EXLUSIVE_OR",
        "LOP_OR",
        "LOP_NEGATED_OR",
        "LOP_EQUIVALENCE",
        "LOP_INVERTED",
        "LOP_REVERSE_OR",
        "LOP_INVERTED_COPY",
        "LOP_INVERTED_OR",
        "LOP_NEGATED_AND",
        "LOP_SET"
    };
    private static final int[] textureByteAlignmentMapping = {2, 2, 2, 4};
    private static final int[] minimumNumberOfVertex = {
    	1, // PRIM_POINT
    	2, // PRIM_LINE
    	2, // PRIM_LINES_STRIPS
    	3, // PRIM_TRIANGLE
    	3, // PRIM_TRIANGLE_STRIPS
    	3, // PRIM_TRIANGLE_FANS
    	2  // PRIM_SPRITES
    };
    private static VideoEngine instance;
    private sceDisplay display;
    private IRenderingEngine re;
    private GeContext context;
    private IREBufferManager bufferManager;
    public static Logger log = Logger.getLogger("ge");
    public static final boolean useTextureCache = true;
    private boolean useVertexCache = false;
    private boolean useAsyncVertexCache = true;
    public boolean useOptimisticVertexCache = false;
    private boolean useTextureAnisotropicFilter = false;
    private static GeCommands helper;
    private int command;
    private int normalArgument;
    private int waitForSyncCount;
    private VertexInfo vinfo = new VertexInfo();
    private VertexInfoReader vertexInfoReader = new VertexInfoReader();
    private static final char SPACE = ' ';
    private DurationStatistics statistics = new CpuDurationStatistics("VideoEngine Statistics");
    private DurationStatistics vertexStatistics = new CpuDurationStatistics("Vertex");
    private DurationStatistics vertexReadingStatistics = new CpuDurationStatistics("Vertex Reading");
    private DurationStatistics drawArraysStatistics = new CpuDurationStatistics("glDrawArrays");
    private DurationStatistics waitSignalStatistics = new DurationStatistics("Wait for GE Signal completion");
    private DurationStatistics waitStallStatistics = new DurationStatistics("Wait on stall");
    private DurationStatistics textureCacheLookupStatistics = new CpuDurationStatistics("Lookup in TextureCache");
    private DurationStatistics vertexCacheLookupStatistics = new CpuDurationStatistics("Lookup in VertexCache");
    private DurationStatistics[] commandStatistics;
    private int errorCount;
    private static final int maxErrorCount = 5; // Abort list processing when detecting more errors
    private boolean isLogTraceEnabled;
    private boolean isLogDebugEnabled;
    private boolean isLogInfoEnabled;
    private boolean isLogWarnEnabled;
    private int primCount;
    private boolean viewportChanged;
    public MatrixUpload projectionMatrixUpload;
    public MatrixUpload modelMatrixUpload;
    public MatrixUpload viewMatrixUpload;
    public MatrixUpload textureMatrixUpload;
    private int boneMatrixIndex;
    private int boneMatrixLinearUpdatedMatrix; // number of updated matrix
    private static final float[] blackColor = new float[]{0, 0, 0, 0};
    private boolean lightingChanged;
    private boolean materialChanged;
    private boolean textureChanged;
    private int[] patch_prim_types = { PRIM_TRIANGLE_STRIPS, PRIM_LINES_STRIPS, PRIM_POINT };
    private boolean clutIsDirty;
    private boolean usingTRXKICK;
    private int maxSpriteHeight;
    private int maxSpriteWidth;
    private boolean depthChanged;
    private boolean scissorChanged;
    // opengl needed information/buffers
    private int textureId = -1;
    private boolean textureFlipped;
    private float textureFlipTranslateY;
    private int[] tmp_texture_buffer32 = new int[1024 * 1024];
    private short[] tmp_texture_buffer16 = new short[1024 * 1024];
    private int[] clut_buffer32 = new int[4096];
    private short[] clut_buffer16 = new short[4096];
    private boolean listHasEnded;
    private PspGeList currentList; // The currently executing list
    private static final int drawBufferSize = 2 * 1024 * 1024 * SIZEOF_FLOAT;
    private int bufferId;
    private int nativeBufferId;
    float[][] bboxVertices;
    private ConcurrentLinkedQueue<PspGeList> drawListQueue;
    private boolean somethingDisplayed;
    private boolean forceLoadGEToScreen;
    private boolean geBufChanged;
    private IAction hleAction;
    private int[] currentCMDValues;
    private int[] currentListCMDValues;
    private boolean bboxWarningDisplayed = false;
    private LinkedList<AddressRange> videoTextures;
    private IntBuffer multiDrawFirst;
    private IntBuffer multiDrawCount;
    private static final int maxMultiDrawElements = 1000;
    private static final String name = "VideoEngine";
    private int maxWaitForSyncCount;

    public static class MatrixUpload {
        private final float[] matrix;
        private boolean changed;
        private int[] matrixIndex;
        private int index;
        private int maxIndex;

        public MatrixUpload(float[] matrix, int matrixWidth, int matrixHeight) {
            changed = true;
            this.matrix = matrix;

            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 4; x++) {
                    matrix[y * 4 + x] = (x == y ? 1 : 0);
                }
            }

            maxIndex = matrixWidth * matrixHeight;
            matrixIndex = new int[maxIndex];
            for (int i = 0; i < maxIndex; i++) {
            	matrixIndex[i] = (i % matrixWidth) + (i / matrixWidth) * 4;
            }
        }

        public void startUpload(int startIndex) {
        	index = startIndex;
        }

        public final boolean uploadValue(float value) {
            if (index >= maxIndex) {
            	if(VideoEngine.getInstance().isLogDebugEnabled) {
					VideoEngine.log(String.format("Ignored Matrix upload value (idx=%08X)", index));
				}
            } else {
	            int i = matrixIndex[index];
	            if (matrix[i] != value) {
	                matrix[i] = value;
	                changed = true;
	            }
            }
            index++;

            return index >= maxIndex;
        }

        public boolean isChanged() {
            return changed;
        }

        public void setChanged(boolean changed) {
            this.changed = changed;
        }
    }

	private class UseVertexCacheSettingsListerner extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setUseVertexCache(value);
		}
	}

	private class UseTextureAnisotropicFilterSettingsListerner extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setUseTextureAnisotropicFilter(value);
		}
	}

    private static void log(String msg) {
        log.debug(msg);
    }

    public static VideoEngine getInstance() {
        if (instance == null) {
            helper = new GeCommands();
            instance = new VideoEngine();
        }
        return instance;
    }

    private VideoEngine() {
        context = new GeContext();
        modelMatrixUpload = new MatrixUpload(context.model_uploaded_matrix, 3, 4);
        viewMatrixUpload = new MatrixUpload(context.view_uploaded_matrix, 3, 4);
        textureMatrixUpload = new MatrixUpload(context.texture_uploaded_matrix, 3, 4);
        projectionMatrixUpload = new MatrixUpload(context.proj_uploaded_matrix, 4, 4);
        boneMatrixLinearUpdatedMatrix = 8;

        commandStatistics = new DurationStatistics[256];
        for (int i = 0; i < commandStatistics.length; i++) {
            commandStatistics[i] = new DurationStatistics(String.format("%-11s", helper.getCommandString(i)));
        }

        drawListQueue = new ConcurrentLinkedQueue<PspGeList>();

        bboxVertices = new float[8][3];
        for (int i = 0; i < 8; i++) {
            bboxVertices[i] = new float[3];
        }

        currentCMDValues = new int[256];
        currentListCMDValues = new int[256];
        videoTextures = new LinkedList<AddressRange>();

        multiDrawFirst = ByteBuffer.allocateDirect(maxMultiDrawElements * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        multiDrawCount = ByteBuffer.allocateDirect(maxMultiDrawElements * 4).order(ByteOrder.nativeOrder()).asIntBuffer();

    }

    /** Called from pspge module */
    public void pushDrawList(PspGeList list) {
    	synchronized (drawListQueue) {
            drawListQueue.add(list);
		}
    }

    /** Called from pspge module */
    public void pushDrawListHead(PspGeList list) {
        // The ConcurrentLinkedQueue type doesn't allow adding
        // objects directly at the head of the queue.

        // This function creates a new array using the given list as it's head
        // and constructs a new ConcurrentLinkedQueue based on it.
        // The actual drawListQueue is then replaced by this new one.
    	synchronized (drawListQueue) {
            int arraySize = drawListQueue.size();

            if (arraySize > 0) {
                PspGeList[] array = drawListQueue.toArray(new PspGeList[arraySize]);

                ConcurrentLinkedQueue<PspGeList> newQueue = new ConcurrentLinkedQueue<PspGeList>();
                PspGeList[] newArray = new PspGeList[arraySize + 1];

                newArray[0] = list;
                for (int i = 0; i < arraySize; i++) {
                    newArray[i + 1] = array[i];
                    newQueue.add(newArray[i]);
                }

                drawListQueue = newQueue;
            } else {    // If the queue is empty.
                drawListQueue.add(list);
            }
		}
    }

    public int numberDrawLists() {
    	int size;

    	synchronized (drawListQueue) {
        	size = drawListQueue.size();
		}

    	return size;
    }

    public boolean hasDrawLists() {
    	boolean isEmpty;

    	synchronized (drawListQueue) {
			isEmpty = drawListQueue.isEmpty();
		}

    	return !isEmpty;
    }

    public boolean hasDrawList(int listAddr) {
    	boolean result = false;
    	boolean waitAndRetry = false;

    	synchronized (drawListQueue) {
            if (currentList != null && currentList.list_addr == listAddr) {
            	result = true;
            	// The current list has already reached the FINISH command,
            	// but the list processing is not yet completed.
            	// Wait a little for the list to complete.
            	if (currentList.isFinished()) {
            		waitAndRetry = true;
            	}
            } else {
	            for (PspGeList list : drawListQueue) {
	                if (list != null && list.list_addr == listAddr) {
	                    result = true;
	                    break;
	                }
	            }
            }
		}

    	if (waitAndRetry) {
    		// The current list is already finished but its processing is not yet
    		// completed. Wait a little (100ms) and check again to avoid
    		// the "can't enqueue duplicate list address" error.
    		for (int i = 0; i < 100; i++) {
    			if (log.isDebugEnabled()) {
    				log.debug(String.format("hasDrawList(0x%08X) waiting on finished list %s", listAddr, currentList));
    			}
    			Utilities.sleep(1, 0);
	    		synchronized (drawListQueue) {
	    			if (currentList == null || currentList.list_addr != listAddr) {
	    				result = false;
	    				break;
	    			}
	    		}
    		}
    	}

    	return result;
    }

    public PspGeList getFirstDrawList() {
    	PspGeList firstList;

    	synchronized (drawListQueue) {
    		firstList = currentList;
        	if (firstList == null) {
        		firstList = drawListQueue.peek();
        	}
		}

    	return firstList;
    }

    public PspGeList getLastDrawList() {
        PspGeList lastList = null;

        synchronized (drawListQueue) {
            for (PspGeList list : drawListQueue) {
                if (list != null) {
                    lastList = list;
                }
            }

            if (lastList == null) {
                lastList = currentList;
            }
		}

        return lastList;
    }

    public void stop() {
    	// If we are still drawing a list, stop the list processing
    	if (currentList != null) {
            synchronized (drawListQueue) {
            	drawListQueue.clear();
			}
    		listHasEnded = true;
    		try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// Ignore Exception
			}
    	}

    	Settings.getInstance().removeSettingsListener(name);
    }

    public void start() {
    	Settings.getInstance().registerSettingsListener(name, "emu.useVertexCache", new UseVertexCacheSettingsListerner());
    	Settings.getInstance().registerSettingsListener(name, "emu.graphics.filters.anisotropic", new UseTextureAnisotropicFilterSettingsListerner());

    	display = Modules.sceDisplayModule;
        re = display.getRenderingEngine();
        re.setGeContext(context);
        context.setRenderingEngine(re);
        bufferManager = re.getBufferManager();

        if (!re.getBufferManager().useVBO()) {
            // VertexCache is relying on VBO
            useVertexCache = false;
        }

        bufferId = bufferManager.genBuffer(IRenderingEngine.RE_FLOAT, drawBufferSize / SIZEOF_FLOAT, IRenderingEngine.RE_STREAM_DRAW);
        nativeBufferId = bufferManager.genBuffer(IRenderingEngine.RE_BYTE, drawBufferSize, IRenderingEngine.RE_STREAM_DRAW);

        if (useAsyncVertexCache) {
        	AsyncVertexCache.getInstance().setUseVertexArray(re.isVertexArrayAvailable());
        }

        context.setDirty();
        projectionMatrixUpload.setChanged(true);
        modelMatrixUpload.setChanged(true);
        viewMatrixUpload.setChanged(true);
        textureMatrixUpload.setChanged(true);
        lightingChanged = true;
        textureChanged = true;
        geBufChanged = true;
        viewportChanged = true;
        depthChanged = true;
        materialChanged = true;
    }

    public IRenderingEngine getRenderingEngine() {
    	return re;
    }

    public GeContext getContext() {
    	return context;
    }

    public static void exit() {
        if (instance != null) {
        	if (instance.re != null) {
        		instance.re.exit();
        	}
        	if (DurationStatistics.collectStatistics) {
	            log.info(instance.statistics);
	            Arrays.sort(instance.commandStatistics);
	            final int numberCommands = 20;
	            log.info(String.format("%d most time intensive Video commands:", numberCommands));
	            for (int i = 0; i < numberCommands; i++) {
	                log.info(String.format("    %s", instance.commandStatistics[i]));
	            }
	            log.info(instance.vertexStatistics);
	            log.info(instance.vertexReadingStatistics);
	            log.info(instance.drawArraysStatistics);
	            log.info(instance.waitSignalStatistics);
	            log.info(instance.waitStallStatistics);
	            log.info(instance.textureCacheLookupStatistics);
	            log.info(instance.vertexCacheLookupStatistics);
	            VertexBufferManager.exit();
	            VertexArrayManager.exit();
        	}
        }
    }

    public static DurationStatistics getStatistics() {
        if (instance == null) {
            return null;
        }

        return instance.statistics;
    }

    /** call from GL thread
     * @return true if an update was made
     */
    public boolean update() {
    	int listCount;
    	synchronized (drawListQueue) {
            listCount = drawListQueue.size();
            currentList = drawListQueue.poll();
		}
        if (currentList == null) {
            return false;
        }

        startUpdate();

        if (State.captureGeNextFrame) {
            CaptureManager.startCapture("capture.bin", currentList);
        }

        if (State.replayGeNextFrame) {
            // Load the replay list into drawListQueue
            CaptureManager.startReplay("capture.bin");

            // Hijack the current list with the replay list
            // TODO this is assuming there is only 1 list in drawListQueue at this point, only the last list is the replay list
            PspGeList replayList = drawListQueue.poll();
            replayList.id = currentList.id;
            replayList.blockedThreadIds.clear();
            replayList.blockedThreadIds.addAll(currentList.blockedThreadIds);
            currentList = replayList;
        }

        // Draw only as many lists as currently available in the drawListQueue.
        // Some game add automatically a new list to the queue when the current
        // list is finishing.
        do {
            executeList();
            listCount--;
            if (listCount <= 0) {
                break;
            }

            synchronized (drawListQueue) {
                currentList = drawListQueue.poll();
			}
        } while (currentList != null);

        currentList = null;

        if (State.captureGeNextFrame) {
            // Can't end capture until we get a sceDisplaySetFrameBuf after the list has executed
            CaptureManager.markListExecuted();
        }

        if (State.replayGeNextFrame) {
            CaptureManager.endReplay();
            State.replayGeNextFrame = false;
        }

        endUpdate();

        return somethingDisplayed;
    }

    private void logLevelUpdated() {
        isLogTraceEnabled = log.isTraceEnabled();
        isLogDebugEnabled = log.isDebugEnabled();
        isLogInfoEnabled = log.isInfoEnabled();
        isLogWarnEnabled = log.isEnabledFor(Level.WARN);
    }

    public void setLogLevel(Level level) {
        log.setLevel(level);
        logLevelUpdated();
    }

    /**
     * The memory used by GE has been updated or changed.
     * Update the caches so that they see these changes.
     */
    private void memoryForGEUpdated() {
        if (useTextureCache) {
            TextureCache.getInstance().resetTextureAlreadyHashed();
        }
        if (useVertexCache) {
            VertexCache.getInstance().resetVertexAlreadyChecked();
        }
        VertexBufferManager.getInstance().resetAddressAlreadyChecked();
    }

    public void hleSetFrameBuf(int topAddr, int bufferWidth, int pixelFormat) {
    	if (context.fbp != topAddr || context.fbw != bufferWidth || context.psm != pixelFormat) {
    		context.fbp = topAddr;
    		context.fbw = bufferWidth;
    		context.psm = pixelFormat;
    		geBufChanged = true;
    	}
    }

    private void startUpdate() {
    	// Wait longer for a sync when the compiler is not enabled... Jpcsp is then much slower
    	maxWaitForSyncCount = RuntimeContext.isCompilerEnabled() ? 100 : 10000;

    	statistics.start();

        logLevelUpdated();
        memoryForGEUpdated();
        somethingDisplayed = false;
        geBufChanged = true;
        forceLoadGEToScreen = true;
        textureChanged = true;
        projectionMatrixUpload.setChanged(true);
        modelMatrixUpload.setChanged(true);
        viewMatrixUpload.setChanged(true);
        textureMatrixUpload.setChanged(true);
        clutIsDirty = true;
        lightingChanged = true;
        viewportChanged = true;
        depthChanged = true;
        materialChanged = true;
        scissorChanged = true;
        errorCount = 0;
        usingTRXKICK = false;
        maxSpriteHeight = 0;
        maxSpriteWidth = 0;
        primCount = 0;

        // Reset all the values
        for (int i = 0; i < currentListCMDValues.length; i++) {
        	currentListCMDValues[i] = -1;
        }

        context.update();
    }

    private void endUpdate() {
    	if (re.isVertexArrayAvailable()) {
    		re.bindVertexArray(0);
    	}

    	context.reTextureGenS.setEnabled(false);
    	context.reTextureGenT.setEnabled(false);

    	if (useVertexCache) {
            if (primCount > VertexCache.cacheMaxSize) {
                log.warn(String.format("VertexCache size (%d) too small to execute %d PRIM commands", VertexCache.cacheMaxSize, primCount));
            }
        }

		statistics.end();
    }

    public void error(String message) {
        errorCount++;
        log.error(message);
        if (errorCount >= maxErrorCount) {
            if (tryToFallback()) {
                log.error("Aborting current list processing due to too many errors");
            }
        }
    }

    private boolean tryToFallback() {
        boolean abort = false;

        if (!currentList.isStackEmpty()) {
            // When have some CALLs on the stack, try to return from the last CALL
            int oldPc = currentList.getPc();
            currentList.ret();
            int newPc = currentList.getPc();
            if (isLogDebugEnabled) {
                log(String.format("tryToFallback old PC: 0x%08X, new PC: 0x%08X", oldPc, newPc));
            }
        } else {
            // Finish this list
            currentList.finishList();
            // Trigger a FINISH callback to avoid hanging the application...
            currentList.pushFinishCallback(currentList.id, 0);
            listHasEnded = true;
            abort = true;
        }

        return abort;
    }

    private void checkCurrentListPc() {
        Memory mem = Memory.getInstance();
        while (!Memory.isAddressGood(currentList.getPc())) {
            if (!mem.isIgnoreInvalidMemoryAccess()) {
                error("Reading GE list from invalid address 0x" + Integer.toHexString(currentList.getPc()));
                break;
            }
			// Ignoring memory read errors.
			// Try to fall back and continue the list processing.
			log.warn("Reading GE list from invalid address 0x" + Integer.toHexString(currentList.getPc()));
			if (tryToFallback()) {
			    break;
			}
        }
    }

    private void executeHleAction() {
        if (hleAction != null) {
            hleAction.execute();
            hleAction = null;
        }
    }

    private void executeListStalled() {
		waitStallStatistics.start();
        if (isLogDebugEnabled) {
            log.debug(String.format("Stall address 0x%08X reached, waiting for Sync", currentList.getPc()));
        }
        currentList.status = PSP_GE_LIST_STALL_REACHED;
		long startWaitClockMillis = Emulator.getClock().milliTime();
        if (!currentList.waitForSync(10)) {
			long endWaitClockMillis = Emulator.getClock().milliTime();
            if (isLogDebugEnabled) {
                log.debug("Wait for sync while stall reached");
            }
		    // Count only when the clock is not paused
		    if (startWaitClockMillis != endWaitClockMillis) {
		    	waitForSyncCount++;
		    }

            // Waiting maximum 100 * 10ms (= 1 second) on a stall address.
            // After this timeout, abort the list.
            //
            // When the stall address is at the very beginning of the list
            // (i.e. the list has just been enqueued, but the stall has not yet been updated),
            // allow waiting for a longer time (the CPU might be busy
            // compiling a huge CodeBlock on the first call).
            // This avoids aborting the first list enqueued.
            int maxStallCount = maxWaitForSyncCount;
            if (currentList.getPc() == currentList.list_addr) {
            	maxStallCount *= 4;
            }
            if (isLogDebugEnabled) {
            	maxStallCount = Integer.MAX_VALUE;
            }

            if (waitForSyncCount > maxStallCount) {
                error(String.format("Waiting too long on stall address 0x%08X, aborting the list %s", currentList.getPc(), currentList));
            }
        } else {
            waitForSyncCount = 0;
        }
        executeHleAction();
        if (!currentList.isStallReached()) {
            currentList.status = PSP_GE_LIST_DRAWING;
        }
    	waitStallStatistics.end();
    }

    private boolean executeListPaused() {
		waitSignalStatistics.start();
		if (isLogDebugEnabled) {
		    log.debug(String.format("FINISH / SIGNAL / END reached, waiting for Sync (%s)", currentList.toString()));
		}
		currentList.status = PSP_GE_LIST_END_REACHED;
		long startWaitClockMillis = Emulator.getClock().milliTime();
		if (!currentList.waitForSync(10)) {
			long endWaitClockMillis = Emulator.getClock().milliTime();
		    if (isLogDebugEnabled) {
		        log.debug("Wait for sync while END reached");
		    }
		    // Count only when the clock is not paused
		    if (startWaitClockMillis != endWaitClockMillis) {
		    	waitForSyncCount++;
		    }

		    // Waiting maximum 100 * 10ms (= 1 second) on an END command.
		    // After this timeout, abort the list.
		    if (waitForSyncCount > maxWaitForSyncCount) {
		        error(String.format("Waiting too long on an END command, aborting the list %s", currentList));
		    }
		} else {
		    waitForSyncCount = 0;
		}

		executeHleAction();
		if (currentList.isRestarted()) {
			currentList.clearRestart();
			currentList.clearPaused();
		}
		if (!currentList.isPaused()) {
            if (currentList.isFinished()) {
                listHasEnded = true;
                return true;
            }
		    currentList.status = PSP_GE_LIST_DRAWING;
		}
		waitSignalStatistics.end();

		return false;
    }

    // call from GL thread
    // There is an issue here with Emulator.pause
    // - We want to stop on errors
    // - But user may also press pause button
    //   - Either continue drawing to the end of the list (bad if the list contains an infinite loop)
    //   - Or we want to be able to restart drawing when the user presses the run button
    private void executeList() {
        listHasEnded = false;
        currentList.status = PSP_GE_LIST_DRAWING;

        if (isLogDebugEnabled) {
            log("executeList " + currentList);
        }

        executeHleAction();

        waitForSyncCount = 0;
        while (!listHasEnded && (!Emulator.pause || State.captureGeNextFrame)) {
            if (currentList.isPaused() || currentList.isEnded()) {
            	if (executeListPaused()) {
            		break;
            	}
            } else if (currentList.isStallReached()) {
            	executeListStalled();
            } else {
                int ins = currentList.readNextInstruction();
                executeCommand(ins);
            }
        }

        if (Emulator.pause && !listHasEnded) {
        	if (isLogInfoEnabled) {
        		VideoEngine.log.info("Emulator paused - cancelling current list id=" + currentList.id);
        	}
            currentList.status = PSP_GE_LIST_CANCEL_DONE;
        }

        // let DONE take priority over STALL_REACHED
        if (listHasEnded) {
            currentList.status = PSP_GE_LIST_END_REACHED;

            // Tested on PSP:
            // A list is only DONE after a combination of FINISH + END.
            if (currentList.isEnded()) {
                currentList.status = PSP_GE_LIST_DONE;
            }
        }

        if (currentList.isDone()) {
        	Modules.sceGe_userModule.hleGeListSyncDone(currentList);
        }

        executeHleAction();
    }

    public PspGeList getCurrentList() {
        return currentList;
    }

    public float[] getMatrix(int mtxtype) {
        float resmtx[] = new float[4 * 4];
        switch (mtxtype) {
            case PSP_GE_MATRIX_BONE0:
            case PSP_GE_MATRIX_BONE1:
            case PSP_GE_MATRIX_BONE2:
            case PSP_GE_MATRIX_BONE3:
            case PSP_GE_MATRIX_BONE4:
            case PSP_GE_MATRIX_BONE5:
            case PSP_GE_MATRIX_BONE6:
            case PSP_GE_MATRIX_BONE7:
                resmtx = context.bone_uploaded_matrix[mtxtype - PSP_GE_MATRIX_BONE0];
                break;
            case PSP_GE_MATRIX_WORLD:
                resmtx = context.model_uploaded_matrix;
                break;
            case PSP_GE_MATRIX_VIEW:
                resmtx = context.view_uploaded_matrix;
                break;
            case PSP_GE_MATRIX_PROJECTION:
                resmtx = context.proj_uploaded_matrix;
                break;
            case PSP_GE_MATRIX_TEXGEN:
                resmtx = context.texture_uploaded_matrix;
                break;
        }

        return resmtx;
    }

    public int getCommandValue(int cmd) {
    	if (cmd < 0 || cmd >= currentCMDValues.length) {
    		return 0;
    	}
        return currentCMDValues[cmd];
    }

    public String commandToString(int cmd) {
        return GeCommands.getInstance().getCommandString(cmd);
    }

    public static int command(int instruction) {
        return (instruction >>> 24);
    }

    private static int intArgument(int instruction) {
        return (instruction & 0x00FFFFFF);
    }

    private static float floatArgument(int normalArgument) {
        return Float.intBitsToFloat(normalArgument << 8);
    }

    private int getClutAddr(int level, int clutNumEntries, int clutEntrySize) {
        return context.tex_clut_addr + (context.tex_clut_start << 4) * clutEntrySize;
    }

    private void readClut() {
        if (!clutIsDirty || context.tex_clut_addr == 0) {
            return;
        }

		if (!Memory.isAddressGood(context.tex_clut_addr)) {
			if (isLogWarnEnabled) {
				log.warn(String.format("Invalid clut address 0x%08X", context.tex_clut_addr));
			}
			return;
		}

        if (context.tex_clut_mode == CMODE_FORMAT_32BIT_ABGR8888) {
            readClut32(0);
        } else {
            readClut16(0);
        }
    }

    public short[] readClut16(int level) {
        int clutNumEntries = context.tex_clut_num_blocks * 16;

        // Update the clut_buffer only if some clut parameters have been changed
        // since last update.
        if (clutIsDirty) {
        	int clutOffset = context.tex_clut_start << 4;
            IMemoryReader memoryReader = MemoryReader.getMemoryReader(getClutAddr(level, clutNumEntries, 2), (clutNumEntries - clutOffset) << 1, 2);
            for (int i = clutOffset; i < clutNumEntries; i++) {
                clut_buffer16[i] = (short) memoryReader.readNext();
            }
            clutIsDirty = false;
        }

        if (State.captureGeNextFrame) {
            log.info("Capture readClut16");
            CaptureManager.captureRAM(context.tex_clut_addr, clutNumEntries * 2);
        }

        return clut_buffer16;
    }

    public int[] readClut32(int level) {
        int clutNumEntries = context.tex_clut_num_blocks * 8;

        // Update the clut_buffer only if some clut parameters have been changed
        // since last update.
        if (clutIsDirty) {
        	int clutOffset = context.tex_clut_start << 4;
            IMemoryReader memoryReader = MemoryReader.getMemoryReader(getClutAddr(level, clutNumEntries, 4), (clutNumEntries - clutOffset) << 2, 4);
            for (int i = clutOffset; i < clutNumEntries; i++) {
                clut_buffer32[i] = memoryReader.readNext();
            }
            clutIsDirty = false;
        }

        if (State.captureGeNextFrame) {
            log.info("Capture readClut32");
            CaptureManager.captureRAM(context.tex_clut_addr, clutNumEntries * 4);
        }

        return clut_buffer32;
    }

    private int getClutIndex(int index) {
        return ((index >> context.tex_clut_shift) & context.tex_clut_mask) | (context.tex_clut_start << 4);
    }

    // UnSwizzling based on pspplayer
    private Buffer unswizzleTextureFromMemory(int texaddr, int bytesPerPixel, int level) {
        int rowWidth = (bytesPerPixel > 0) ? (context.texture_buffer_width[level] * bytesPerPixel) : (context.texture_buffer_width[level] / 2);
        int pitch = rowWidth / 4;
        int bxc = rowWidth / 16;
        int byc = Math.max((context.texture_height[level] + 7) / 8, 1);

        int ydest = 0;

        IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, 4);
        for (int by = 0; by < byc; by++) {
            if (rowWidth >= 16) {
                int xdest = ydest;
                for (int bx = 0; bx < bxc; bx++) {
                    int dest = xdest;
                    for (int n = 0; n < 8; n++) {
                        tmp_texture_buffer32[dest] = memoryReader.readNext();
                        tmp_texture_buffer32[dest + 1] = memoryReader.readNext();
                        tmp_texture_buffer32[dest + 2] = memoryReader.readNext();
                        tmp_texture_buffer32[dest + 3] = memoryReader.readNext();

                        dest += pitch;
                    }
                    xdest += 4;
                }
                ydest += (rowWidth * 8) / 4;
            } else if (rowWidth == 8) {
            	for (int n = 0; n < 8; n++, ydest += 2) {
                    tmp_texture_buffer32[ydest] = memoryReader.readNext();
                    tmp_texture_buffer32[ydest + 1] = memoryReader.readNext();
                    memoryReader.skip(2);
            	}
            } else if (rowWidth == 4) {
            	for (int n = 0; n < 8; n++, ydest++) {
                    tmp_texture_buffer32[ydest] = memoryReader.readNext();
                    memoryReader.skip(3);
            	}
            } else if (rowWidth == 2) {
            	for (int n = 0; n < 4; n++, ydest++) {
            		int n1 = memoryReader.readNext() & 0xFFFF;
            		memoryReader.skip(3);
            		int n2 = memoryReader.readNext() & 0xFFFF;
                    memoryReader.skip(3);
                    tmp_texture_buffer32[ydest] = n1 | (n2 << 16);
            	}
            } else if (rowWidth == 1) {
            	for (int n = 0; n < 2; n++, ydest++) {
            		int n1 = memoryReader.readNext() & 0xFF;
            		memoryReader.skip(3);
            		int n2 = memoryReader.readNext() & 0xFF;
                    memoryReader.skip(3);
            		int n3 = memoryReader.readNext() & 0xFF;
                    memoryReader.skip(3);
            		int n4 = memoryReader.readNext() & 0xFF;
                    memoryReader.skip(3);
                    tmp_texture_buffer32[ydest] = n1 | (n2 << 8) | (n3 << 16) | (n4 << 24);
            	}
            }
        }

        if (State.captureGeNextFrame) {
            log.info("Capture unswizzleTextureFromMemory");
            CaptureManager.captureRAM(texaddr, rowWidth * context.texture_height[level]);
        }

        return IntBuffer.wrap(tmp_texture_buffer32);
    }

    private String getArgumentLog(int normalArgument) {
        if (normalArgument == 0) {
            return "(0)"; // a very common case...
        }

        return String.format("(hex=%08X,int=%d,float=%f)", normalArgument, normalArgument, floatArgument(normalArgument));
    }

    public void executeCommand(int instruction) {
        command = command(instruction);

        // Quick check: pure state commands can be ignored when they are
        // repeated with the same parameters. These are redundant commands.
        if (GeCommands.pureStateCommands[command]) {
        	if (currentListCMDValues[command] == instruction) {
        		if (isLogDebugEnabled) {
    	        	log.debug(String.format("%s 0x%06X redundant pure state cmd ignored", helper.getCommandString(command), intArgument(instruction)));
        		}
        		return;
        	}
        	currentListCMDValues[command] = instruction;
        }

        normalArgument = intArgument(instruction);
        // Compute floatArgument only on demand, most commands do not use it.
        //float floatArgument = floatArgument(instruction);

        currentCMDValues[command] = normalArgument;
        if (DurationStatistics.collectStatistics) {
            commandStatistics[command].start();
        }
        switch (command) {
	        case NOP: executeCommandNOP(); break;
	        case VADDR: executeCommandVADDR(); break;
	        case IADDR: executeCommandIADDR(); break;
	        case PRIM: executeCommandPRIM(); break;
	        case BEZIER: executeCommandBEZIER(); break;
	        case SPLINE: executeCommandSPLINE(); break;
	        case BBOX: executeCommandBBOX(); break;
	        case JUMP: executeCommandJUMP(); break;
	        case BJUMP: executeCommandBJUMP(); break;
	        case CALL: executeCommandCALL(); break;
	        case RET: executeCommandRET(); break;
	        case END: executeCommandEND(); break;
	        case SIGNAL: executeCommandSIGNAL(); break;
	        case FINISH: executeCommandFINISH(); break;
	        case BASE: executeCommandBASE(); break;
	        case VTYPE: executeCommandVTYPE(); break;
	        case OFFSET_ADDR: executeCommandOFFSET_ADDR(); break;
	        case ORIGIN_ADDR: executeCommandORIGIN_ADDR(); break;
	        case REGION1: executeCommandREGION1(); break;
	        case REGION2: executeCommandREGION2(); break;
	        case LTE: executeCommandLTE(); break;
	        case LTE0:
	        case LTE1:
	        case LTE2:
	        case LTE3: executeCommandLTEn(); break;
	        case CPE: executeCommandCPE(); break;
	        case BCE: executeCommandBCE(); break;
	        case TME: executeCommandTME(); break;
	        case FGE: executeCommandFGE(); break;
	        case DTE: executeCommandDTE(); break;
	        case ABE: executeCommandABE(); break;
	        case ATE: executeCommandATE(); break;
	        case ZTE: executeCommandZTE(); break;
	        case STE: executeCommandSTE(); break;
	        case AAE: executeCommandAAE(); break;
	        case PCE: executeCommandPCE(); break;
	        case CTE: executeCommandCTE(); break;
	        case LOE: executeCommandLOE(); break;
	        case BOFS: executeCommandBOFS(); break;
	        case BONE: executeCommandBONE(); break;
	        case MW0:
	        case MW1:
	        case MW2:
	        case MW3:
	        case MW4:
	        case MW5:
	        case MW6:
	        case MW7: executeCommandMWn(); break;
	        case PSUB: executeCommandPSUB(); break;
	        case PPRIM: executeCommandPPRIM(); break;
	        case PFACE: executeCommandPFACE(); break;
	        case MMS: executeCommandMMS(); break;
	        case MODEL: executeCommandMODEL(); break;
	        case VMS: executeCommandVMS(); break;
	        case VIEW: executeCommandVIEW(); break;
	        case PMS: executeCommandPMS(); break;
	        case PROJ: executeCommandPROJ(); break;
	        case TMS: executeCommandTMS(); break;
	        case TMATRIX: executeCommandTMATRIX(); break;
	        case XSCALE: executeCommandXSCALE(); break;
	        case YSCALE: executeCommandYSCALE(); break;
	        case ZSCALE: executeCommandZSCALE(); break;
	        case XPOS: executeCommandXPOS(); break;
	        case YPOS: executeCommandYPOS(); break;
	        case ZPOS: executeCommandZPOS(); break;
	        case USCALE: executeCommandUSCALE(); break;
	        case VSCALE: executeCommandVSCALE(); break;
	        case UOFFSET: executeCommandUOFFSET(); break;
	        case VOFFSET: executeCommandVOFFSET(); break;
	        case OFFSETX: executeCommandOFFSETX(); break;
	        case OFFSETY: executeCommandOFFSETY(); break;
	        case SHADE: executeCommandSHADE(); break;
	        case RNORM: executeCommandRNORM(); break;
	        case CMAT: executeCommandCMAT(); break;
	        case EMC: executeCommandEMC(); break;
	        case AMC: executeCommandAMC(); break;
	        case DMC: executeCommandDMC(); break;
	        case SMC: executeCommandSMC(); break;
	        case AMA: executeCommandAMA(); break;
	        case SPOW: executeCommandSPOW(); break;
	        case ALC: executeCommandALC(); break;
	        case ALA: executeCommandALA(); break;
	        case LMODE: executeCommandLMODE(); break;
	        case LT0:
	        case LT1:
	        case LT2:
	        case LT3: executeCommandLTn(); break;
	        case LXP0:
	        case LXP1:
	        case LXP2:
	        case LXP3:
	        case LYP0:
	        case LYP1:
	        case LYP2:
	        case LYP3:
	        case LZP0:
	        case LZP1:
	        case LZP2:
	        case LZP3: executeCommandLXPn(); break;
	        case LXD0:
	        case LXD1:
	        case LXD2:
	        case LXD3:
	        case LYD0:
	        case LYD1:
	        case LYD2:
	        case LYD3:
	        case LZD0:
	        case LZD1:
	        case LZD2:
	        case LZD3: executeCommandLXDn(); break;
	        case LCA0:
	        case LCA1:
	        case LCA2:
	        case LCA3: executeCommandLCAn(); break;
	        case LLA0:
	        case LLA1:
	        case LLA2:
	        case LLA3: executeCommandLLAn(); break;
	        case LQA0:
	        case LQA1:
	        case LQA2:
	        case LQA3: executeCommandLQAn(); break;
	        case SLE0:
	        case SLE1:
	        case SLE2:
	        case SLE3: executeCommandSLEn(); break;
	        case SLF0:
	        case SLF1:
	        case SLF2:
	        case SLF3: executeCommandSLFn(); break;
	        case ALC0:
	        case ALC1:
	        case ALC2:
	        case ALC3: executeCommandALCn(); break;
	        case DLC0:
	        case DLC1:
	        case DLC2:
	        case DLC3: executeCommandDLCn(); break;
	        case SLC0:
	        case SLC1:
	        case SLC2:
	        case SLC3: executeCommandSLCn(); break;
	        case FFACE: executeCommandFFACE(); break;
	        case FBP: executeCommandFBP(); break;
	        case FBW: executeCommandFBW(); break;
	        case ZBP: executeCommandZBP(); break;
	        case ZBW: executeCommandZBW(); break;
	        case TBP0:
	        case TBP1:
	        case TBP2:
	        case TBP3:
	        case TBP4:
	        case TBP5:
	        case TBP6:
	        case TBP7: executeCommandTBPn(); break;
	        case TBW0:
	        case TBW1:
	        case TBW2:
	        case TBW3:
	        case TBW4:
	        case TBW5:
	        case TBW6:
	        case TBW7: executeCommandTBWn(); break;
	        case CBP: executeCommandCBP(); break;
	        case CBPH: executeCommandCBPH(); break;
	        case TRXSBP: executeCommandTRXSBP(); break;
	        case TRXSBW: executeCommandTRXSBW(); break;
	        case TRXDBP: executeCommandTRXDBP(); break;
	        case TRXDBW: executeCommandTRXDBW(); break;
	        case TSIZE0:
	        case TSIZE1:
	        case TSIZE2:
	        case TSIZE3:
	        case TSIZE4:
	        case TSIZE5:
	        case TSIZE6:
	        case TSIZE7: executeCommandTSIZEn(); break;
	        case TMAP: executeCommandTMAP(); break;
	        case TEXTURE_ENV_MAP_MATRIX: executeCommandTEXTURE_ENV_MAP_MATRIX(); break;
	        case TMODE: executeCommandTMODE(); break;
	        case TPSM: executeCommandTPSM(); break;
	        case CLOAD: executeCommandCLOAD(); break;
	        case CMODE: executeCommandCMODE(); break;
	        case TFLT: executeCommandTFLT(); break;
	        case TWRAP: executeCommandTWRAP(); break;
	        case TBIAS: executeCommandTBIAS(); break;
	        case TFUNC: executeCommandTFUNC(); break;
	        case TEC: executeCommandTEC(); break;
	        case TFLUSH: executeCommandTFLUSH(); break;
	        case TSYNC: executeCommandTSYNC(); break;
	        case FFAR: executeCommandFFAR(); break;
	        case FDIST: executeCommandFDIST(); break;
	        case FCOL: executeCommandFCOL(); break;
	        case TSLOPE: executeCommandTSLOPE(); break;
	        case PSM: executeCommandPSM(); break;
	        case CLEAR: executeCommandCLEAR(); break;
	        case SCISSOR1: executeCommandSCISSOR1(); break;
	        case SCISSOR2: executeCommandSCISSOR2(); break;
	        case NEARZ: executeCommandNEARZ(); break;
	        case FARZ: executeCommandFARZ(); break;
	        case CTST: executeCommandCTST(); break;
	        case CREF: executeCommandCREF(); break;
	        case CMSK: executeCommandCMSK(); break;
	        case ATST: executeCommandATST(); break;
	        case STST: executeCommandSTST(); break;
	        case SOP: executeCommandSOP(); break;
	        case ZTST: executeCommandZTST(); break;
	        case ALPHA: executeCommandALPHA(); break;
	        case SFIX: executeCommandSFIX(); break;
	        case DFIX: executeCommandDFIX(); break;
	        case DTH0: executeCommandDTH0(); break;
	        case DTH1: executeCommandDTH1(); break;
	        case DTH2: executeCommandDTH2(); break;
	        case DTH3: executeCommandDTH3(); break;
	        case LOP: executeCommandLOP(); break;
	        case ZMSK: executeCommandZMSK(); break;
	        case PMSKC: executeCommandPMSKC(); break;
	        case PMSKA: executeCommandPMSKA(); break;
	        case TRXKICK: executeCommandTRXKICK(); break;
	        case TRXPOS: executeCommandTRXPOS(); break;
	        case TRXDPOS: executeCommandTRXDPOS(); break;
	        case TRXSIZE: executeCommandTRXSIZE(); break;
	        case VSCX: executeCommandVSCX(); break;
	        case VSCY: executeCommandVSCY(); break;
	        case VSCZ: executeCommandVSCZ(); break;
	        case VTCS: executeCommandVTCS(); break;
	        case VTCT: executeCommandVTCT(); break;
	        case VTCQ: executeCommandVTCQ(); break;
	        case VCV: executeCommandVCV(); break;
	        case VAP: executeCommandVAP(); break;
	        case VFC: executeCommandVFC(); break;
	        case VSCV: executeCommandVSCV(); break;
	        case DUMMY: executeCommandDUMMY(); break;
	        default: executeCommandUNKNOWN(); break;
        }
        if (DurationStatistics.collectStatistics) {
            commandStatistics[command].end();
        }
    }

    private void executeCommandUNKNOWN() {
        if (isLogWarnEnabled) {
            log.warn(String.format("Unknown/unimplemented video command [%s]%s at 0x%08X", helper.getCommandString(command), getArgumentLog(normalArgument), currentList.getPc() - 4));
        }
    }

    private void executeCommandCLEAR() {
        if ((normalArgument & 1) == 0) {
            re.endClearMode();
            if (isLogDebugEnabled) {
            	log("clear mode end");
            }
        } else {
            // TODO Add more disabling in clear mode, we also need to reflect the change to the internal GE registers
            boolean color = (normalArgument & 0x100) != 0;
            boolean alpha = (normalArgument & 0x200) != 0;
            boolean depth = (normalArgument & 0x400) != 0;

            updateGeBuf();
            re.startClearMode(color, alpha, depth);
            if (isLogDebugEnabled) {
                log("clear mode : " + (normalArgument >> 8));
            }
        }

        lightingChanged = true;
        projectionMatrixUpload.setChanged(true);
        modelMatrixUpload.setChanged(true);
        viewMatrixUpload.setChanged(true);
        textureMatrixUpload.setChanged(true);
        viewportChanged = true;
        depthChanged = true;
        materialChanged = true;
    }

    private void executeCommandTFUNC() {
    	context.textureFunc = normalArgument & 0x7;
    	if (context.textureFunc >= TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_UNKNOW1) {
            VideoEngine.log.warn("Unimplemented tfunc mode " + context.textureFunc);
            context.textureFunc = TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_MODULATE;
    	}

    	context.textureAlphaUsed = ((normalArgument >> 8) & 0x1) != TFUNC_FRAGMENT_DOUBLE_TEXTURE_COLOR_ALPHA_IS_IGNORED;
    	context.textureColorDoubled = ((normalArgument >> 16) & 0x1) != TFUNC_FRAGMENT_DOUBLE_ENABLE_COLOR_UNTOUCHED;

        re.setTextureFunc(context.textureFunc, context.textureAlphaUsed, context.textureColorDoubled);

        if (isLogDebugEnabled) {
            log(String.format("sceGuTexFunc mode %06X", normalArgument)
                    + (((normalArgument & 0x10000) != 0) ? " SCALE" : "")
                    + (((normalArgument & 0x100) != 0) ? " ALPHA" : ""));
        }
    }

    private int fixNativeBufferOffset(Buffer vertexData, int size) {
    	// Handle buffer address not aligned with memory Buffer object.
    	// E.g. ptr_vertex = 0xNNNNNN2 and vertexData is an IntBuffer
    	// starting at 0xNNNNNN0
    	int nativeBufferOffset = 0;
    	if (vertexData instanceof IntBuffer || vertexData instanceof FloatBuffer) {
    		nativeBufferOffset = vinfo.ptr_vertex & 3;
    	} else if (vertexData instanceof ShortBuffer) {
    		nativeBufferOffset = vinfo.ptr_vertex & 1;
    	}
    	size += nativeBufferOffset;
    	vertexInfoReader.addNativeOffset(nativeBufferOffset);

    	return size;
    }

    private int checkMultiDraw(int currentFirst, int currentType, int currentNumberOfVertex, IntBuffer bufferFirst, IntBuffer bufferCount) {
    	if (isLogDebugEnabled) {
    		log(String.format("checkMultiDraw at 0x%08X", currentList.getPc()));
    	}
    	Memory mem = Memory.getInstance();
    	int pc = currentList.getPc();
    	int afterMultiPc = pc;
    	boolean hasMultiDraw = false;
    	int initialFirst = currentFirst;
    	int currentSkip = 0;
    	bufferFirst.clear();
    	bufferCount.clear();
    	int currentPtrVertex = vinfo.ptr_vertex + vinfo.vertexSize * currentNumberOfVertex;
    	boolean frontFaceCw = context.frontFaceCw;

    	// Leave at least one entry free to put the last item
    	while (bufferFirst.remaining() > 1) {
    		int instruction = mem.read32(pc);
    		pc += 4;
    		int cmd = command(instruction);
    		if (cmd == PRIM) {
    			if (context.frontFaceCw != frontFaceCw) {
    				if (isLogDebugEnabled) {
        	        	log.debug(String.format("%s 0x%06X non matching FFACE has stopped integration in MultiDrawArrays", helper.getCommandString(cmd), intArgument(instruction)));
    				}
    				break;
    			}
    	        int type = ((instruction >> 16) & 0x7);
    	        if (type != currentType) {
    				if (isLogDebugEnabled) {
        	        	log.debug(String.format("%s 0x%06X non matching vertex type has stopped integration in MultiDrawArrays", helper.getCommandString(cmd), intArgument(instruction)));
    				}
    	        	break;
    	        }
    	        int numberOfVertex = instruction & 0xFFFF;
				bufferFirst.put(currentFirst);
    	        bufferCount.put(currentNumberOfVertex);
    	        currentFirst += currentNumberOfVertex + currentSkip;
    	        currentNumberOfVertex = numberOfVertex;
    	        currentPtrVertex += vinfo.vertexSize * (numberOfVertex + currentSkip);
    	        currentSkip = 0;
    	        hasMultiDraw = true;
    	        afterMultiPc = pc;
    	        if (isLogDebugEnabled) {
    	        	log.debug(String.format("%s type=%d, numberOfVertex=%d integrated in MultiDrawArrays", helper.getCommandString(cmd), type, numberOfVertex));
    	        }
    		} else if (cmd == VADDR) {
    			int arg = intArgument(instruction);
    	        int ptr_vertex = currentList.getAddressRelOffset(arg);
    			if (ptr_vertex == currentPtrVertex) {
    				// VADDR in sequence, skip the command
        	        if (isLogDebugEnabled) {
        	        	log.debug(String.format("%s 0x%08X integrated in MultiDrawArrays", helper.getCommandString(cmd), ptr_vertex));
        	        }
    			} else if (ptr_vertex > currentPtrVertex && ((ptr_vertex - currentPtrVertex) % vinfo.vertexSize) == 0) {
    				// VADDR almost in sequence with an aligned hole, skip the command
    				currentSkip = (ptr_vertex - currentPtrVertex) / vinfo.vertexSize;
        	        if (isLogDebugEnabled) {
        	        	log.debug(String.format("%s 0x%08X (skip=%d) integrated in MultiDrawArrays", helper.getCommandString(cmd), ptr_vertex, currentSkip));
        	        }
    			} else {
        	        if (isLogDebugEnabled) {
        	        	log.debug(String.format("%s 0x%08X not integrated in MultiDrawArrays (needed 0x%08X)", helper.getCommandString(cmd), ptr_vertex, currentPtrVertex));
        	        }
    				break;
    			}
    		} else if (cmd == TBIAS) {
    			int tex_mipmap_mode = instruction & 0x3;
    			if (context.tex_mipmap_mode == tex_mipmap_mode && tex_mipmap_mode == TBIAS_MODE_AUTO) {
    				// Skip TBIAS with TBIAS_MODE_AUTO, ignore tex_mipmap_bias parameter
    				if (isLogDebugEnabled) {
        	        	log.debug(String.format("%s 0x%06X integrated in MultiDrawArrays", helper.getCommandString(cmd), intArgument(instruction)));
    				}
    			} else {
    				if (isLogDebugEnabled) {
        	        	log.debug(String.format("%s 0x%06X has stopped integration in MultiDrawArrays", helper.getCommandString(cmd), intArgument(instruction)));
    				}
    				break;
    			}
    		} else if (GeCommands.pureStateCommands[cmd]) {
    			if (cmd == FFACE) {
    				// Some applications generate the following sequence:
    				//   FFACE 0
    				//   PRIM xxx
    				//   FFACE 1
    				//   FFACE 0
    				//   PRIM xxx
    				// Detect such sequences (changing the FFACE with no effect)
    				// and integrate them in multiDraw.
    				frontFaceCw = intArgument(instruction) != 0;
    				if (isLogDebugEnabled) {
        	        	log.debug(String.format("%s 0x%06X trying to integrate in MultiDrawArrays", helper.getCommandString(cmd), intArgument(instruction)));
    				}
    			} else if (currentListCMDValues[cmd] == instruction) {
    				// The command has been repeated with the same parameters,
    				// it can be ignored.
    				if (isLogDebugEnabled) {
        	        	log.debug(String.format("%s 0x%06X pure state cmd integrated in MultiDrawArrays", helper.getCommandString(cmd), intArgument(instruction)));
    				}
    			} else {
    				if (isLogDebugEnabled) {
        	        	log.debug(String.format("%s 0x%06X pure state cmd has stopped integration in MultiDrawArrays", helper.getCommandString(cmd), intArgument(instruction)));
    				}
    				break;
    			}
    		} else {
				if (isLogDebugEnabled) {
    	        	log.debug(String.format("%s 0x%06X has stopped integration in MultiDrawArrays", helper.getCommandString(cmd), intArgument(instruction)));
				}
    			break;
    		}
    	}

    	if (!hasMultiDraw) {
    		return -1;
    	}

    	bufferFirst.put(currentFirst);
		bufferCount.put(currentNumberOfVertex);

		bufferFirst.limit(bufferFirst.position());
		bufferFirst.rewind();
		bufferCount.limit(bufferCount.position());
		bufferCount.rewind();

		currentList.setPc(afterMultiPc);

    	return currentFirst + currentNumberOfVertex - initialFirst;
    }

    private void executeCommandPRIM() {
        int numberOfVertex = normalArgument & 0xFFFF;
        int type = ((normalArgument >> 16) & 0x7);

        if (numberOfVertex == 0) {
        	return;
        }

        Memory mem = Memory.getInstance();
        if (!Memory.isAddressGood(vinfo.ptr_vertex)) {
            // Abort here to avoid a lot of useless memory read errors...
            error(helper.getCommandString(PRIM) + " Invalid vertex address 0x" + Integer.toHexString(vinfo.ptr_vertex));
            return;
        }

        if (type > PRIM_SPRITES) {
            error(String.format("%s: Type %d unhandled at 0x%08X", helper.getCommandString(PRIM), type, currentList.getPc() - 4));
            return;
        }

        if (numberOfVertex < minimumNumberOfVertex[type]) {
        	if (isLogDebugEnabled) {
        		log.debug(String.format("%s type %d unsufficient number of vertex %d", helper.getCommandString(PRIM), type, numberOfVertex));
        	}
        	return;
        }

        updateGeBuf();
        somethingDisplayed = true;
        primCount++;

        loadTexture();

        // Logging
        if (isLogDebugEnabled) {
            switch (type) {
                case PRIM_POINT:
                    log("prim point " + numberOfVertex + "x");
                    break;
                case PRIM_LINE:
                    log("prim line " + (numberOfVertex / 2) + "x");
                    break;
                case PRIM_LINES_STRIPS:
                    log("prim lines_strips " + (numberOfVertex - 1) + "x");
                    break;
                case PRIM_TRIANGLE:
                    log("prim triangle " + (numberOfVertex / 3) + "x");
                    break;
                case PRIM_TRIANGLE_STRIPS:
                    log("prim triangle_strips " + (numberOfVertex - 2) + "x");
                    break;
                case PRIM_TRIANGLE_FANS:
                    log("prim triangle_fans " + (numberOfVertex - 2) + "x");
                    break;
                case PRIM_SPRITES:
                    log("prim sprites " + (numberOfVertex / 2) + "x");
                    break;
                default:
                    VideoEngine.log.warn("prim unhandled " + type);
                    break;
            }
        }

        boolean useVertexColor = initRendering();

        int nTexCoord = 2;
        int nColor = 4;
        int nVertex = 3;

        boolean useTexture = false;
        boolean useTextureFromNormal = false;
        boolean useTextureFromNormalizedNormal = false;
        boolean useTextureFromPosition = false;
        switch (context.tex_map_mode) {
            case TMAP_TEXTURE_MAP_MODE_TEXTURE_COORDIATES_UV:
            	if (vinfo.texture != 0) {
            	    useTexture = true;
            	}
                break;

            case TMAP_TEXTURE_MAP_MODE_TEXTURE_MATRIX: {
                switch (context.tex_proj_map_mode) {
                    case TMAP_TEXTURE_PROJECTION_MODE_POSITION:
                        if (vinfo.position != 0) {
                            useTexture = true;
                            useTextureFromPosition = true;
                	        nTexCoord = nVertex;
                        }
                        break;
                    case TMAP_TEXTURE_PROJECTION_MODE_TEXTURE_COORDINATES:
                        if (vinfo.texture != 0) {
                            useTexture = true;
                        }
                        break;
                    case TMAP_TEXTURE_PROJECTION_MODE_NORMAL:
                        if (vinfo.normal != 0) {
                            useTexture = true;
                            useTextureFromNormal = true;
                            nTexCoord = 3;
                        }
                        break;
                    case TMAP_TEXTURE_PROJECTION_MODE_NORMALIZED_NORMAL:
                        if (vinfo.normal != 0) {
                            useTexture = true;
                            useTextureFromNormalizedNormal = true;
                            nTexCoord = 3;
                        }
                        break;
                }
                break;
            }

            case TMAP_TEXTURE_MAP_MODE_ENVIRONMENT_MAP:
                break;

            default:
                log("Unhandled texture matrix mode " + context.tex_map_mode);
                break;
        }

    	vertexStatistics.start();

        vinfo.setMorphWeights(context.morph_weight);
        vinfo.setDirty();

        int numberOfWeightsForBuffer;
        boolean mustComputeWeights;
        if (vinfo.weight != 0) {
        	numberOfWeightsForBuffer = re.setBones(vinfo.skinningWeightCount, context.boneMatrixLinear);
        	mustComputeWeights = (numberOfWeightsForBuffer == 0);
        } else {
        	numberOfWeightsForBuffer = re.setBones(0, null);
        	mustComputeWeights = false;
        }

    	if (maxSpriteWidth == 0 || context.scissor_x2 < maxSpriteWidth) {
    		maxSpriteWidth = context.scissor_x2;
    	}
    	if (maxSpriteHeight == 0 || context.scissor_y2 < maxSpriteHeight) {
    		maxSpriteHeight = context.scissor_y2;
    	}

        boolean needSetDataPointers = true;

        // Do not use optimized VertexInfo reading when
        // - using Vertex Cache unless all the vertices are supported natively
        // - the Vertex are indexed
        // - the PRIM_SPRITE primitive is used where it is not supported natively
        // - the normals have to be normalized for the texture mapping
        // - the weights have to be computed and are not supported natively
        // - the vertex address is invalid
        if ((!useVertexCache || re.canAllNativeVertexInfo()) &&
            vinfo.index == 0 &&
            vinfo.morphingVertexCount == 1 &&
            (type != PRIM_SPRITES || re.canNativeSpritesPrimitive()) &&
            !useTextureFromNormalizedNormal &&
            !mustComputeWeights &&
            Memory.isAddressGood(vinfo.ptr_vertex)) {
        	//
            // Optimized VertexInfo reading:
            // - do not copy the info already available in the OpenGL format
            //   (native format), load it into nativeBuffer (a direct buffer
            //   is required by OpenGL).
            // - try to keep the info in "int" format when possible, convert
            //   to "float" only when necessary
            // The best case is no reading and no conversion at all when all the
            // vertex info are available in a format usable by OpenGL.
            //
    		vertexReadingStatistics.start();
            Buffer buffer = vertexInfoReader.read(vinfo, vinfo.ptr_vertex, numberOfVertex, re.canAllNativeVertexInfo());
        	vertexReadingStatistics.end();

        	int stride;
        	int size = vinfo.vertexSize * numberOfVertex;
        	int firstVertex = 0;
        	boolean useBufferManager;
        	boolean multiDrawArrays = false;
        	if (useVertexCache && buffer == null) {
        		stride = vinfo.vertexSize;
        		useBufferManager = false;
        		final int vertexAddress = vinfo.ptr_vertex;
        		VertexBuffer vertexBuffer = VertexBufferManager.getInstance().getVertexBuffer(re, vertexAddress, size, stride, re.isVertexArrayAvailable());
        		Buffer vertexData = mem.getBuffer(vertexAddress, size);
        		vertexBuffer.load(re, vertexData, vertexAddress, size);
        		if (re.isVertexArrayAvailable()) {
        			VertexArray vertexArray = VertexArrayManager.getInstance().getVertexArray(re, vinfo.vtype, vertexBuffer, vertexAddress, stride);
    				needSetDataPointers = vertexArray.bind(re);
    				firstVertex = vertexArray.getVertexOffset(vertexAddress);
        		} else {
            		vertexInfoReader.addNativeOffset(vertexBuffer.getBufferOffset(vertexAddress));
        		}

        		// Check if multiple PRIM's are defined in sequence and
        		// try to merge them into a single multiDrawArrays call.
        		int multiDrawNumberOfVertex = checkMultiDraw(firstVertex, type, numberOfVertex, multiDrawFirst, multiDrawCount);
				if (multiDrawNumberOfVertex > 0) {
					multiDrawArrays = true;
					numberOfVertex = multiDrawNumberOfVertex;
					size = vinfo.vertexSize * multiDrawNumberOfVertex;
	        		vertexData = mem.getBuffer(vertexAddress, size);
					vertexBuffer.load(re, vertexData, vertexAddress, size);
				}

				if (needSetDataPointers) {
        			vertexBuffer.bind(re);
        		}
        	} else {
        		if (re.isVertexArrayAvailable()) {
    				re.bindVertexArray(0);
        		}
                stride = vertexInfoReader.getStride();
                useBufferManager = true;

                if (buffer != null) {
	            	bufferManager.setBufferData(bufferId, stride * numberOfVertex, buffer, IRenderingEngine.RE_STREAM_DRAW);
	            }

	            if (vertexInfoReader.hasNative()) {
	                // Copy the VertexInfo from Memory to the nativeBuffer
	                // (a direct buffer is required by glXXXPointer())
	        		Buffer vertexData = mem.getBuffer(vinfo.ptr_vertex, size);
	        		size = fixNativeBufferOffset(vertexData, size);
	            	bufferManager.setBufferData(nativeBufferId, size, vertexData, IRenderingEngine.RE_STREAM_DRAW);
	            }
        	}

            re.setVertexInfo(vinfo, re.canAllNativeVertexInfo(), useVertexColor, useTexture, type);

            if (needSetDataPointers) {
	            if (vinfo.texture != 0 || useTexture) {
	                boolean textureNative;
	                int textureOffset;
	                int textureType;
	                if (useTextureFromNormal) {
	                    textureNative = vertexInfoReader.isNormalNative();
	                    textureOffset = vertexInfoReader.getNormalOffset();
	                    textureType = vertexInfoReader.getNormalType();
	                    nTexCoord = vertexInfoReader.getNormalNumberValues();
	                } else if (useTextureFromPosition) {
	                    textureNative = vertexInfoReader.isPositionNative();
	                    textureOffset = vertexInfoReader.getPositionOffset();
	                    textureType = vertexInfoReader.getPositionType();
	                    nTexCoord = vertexInfoReader.getPositionNumberValues();
	                } else {
	                    textureNative = vertexInfoReader.isTextureNative();
	                    textureOffset = vertexInfoReader.getTextureOffset();
	                    textureType = vertexInfoReader.getTextureType();
	                    nTexCoord = vertexInfoReader.getTextureNumberValues();
	                }
	                setTexCoordPointer(useTexture, nTexCoord, textureType, stride, textureOffset, textureNative, useBufferManager);
	            }
	            nVertex = vertexInfoReader.getPositionNumberValues();
	            nColor = vertexInfoReader.getColorNumberValues();
	            int nWeight = vertexInfoReader.getWeightNumberValues();

	            enableClientState(useVertexColor, useTexture);
	            setColorPointer(useVertexColor, nColor, vertexInfoReader.getColorType(), stride, vertexInfoReader.getColorOffset(), vertexInfoReader.isColorNative(), useBufferManager);
	            setNormalPointer(vertexInfoReader.getNormalType(), stride, vertexInfoReader.getNormalOffset(), vertexInfoReader.isNormalNative(), useBufferManager);
	            setWeightPointer(nWeight, vertexInfoReader.getWeightType(), stride, vertexInfoReader.getWeightOffset(), vertexInfoReader.isWeightNative(), useBufferManager);
	            setVertexPointer(nVertex, vertexInfoReader.getPositionType(), stride, vertexInfoReader.getPositionOffset(), vertexInfoReader.isPositionNative(), useBufferManager);
        	}

        	drawArraysStatistics.start();
        	if (multiDrawArrays) {
        		re.multiDrawArrays(type, multiDrawFirst, multiDrawCount);
        	} else {
        		re.drawArrays(type, firstVertex, numberOfVertex);
        	}
        	drawArraysStatistics.end();
        } else {
            // Non-optimized VertexInfo reading
            VertexInfo cachedVertexInfo = null;
            if (useVertexCache) {
        		vertexCacheLookupStatistics.start();
                cachedVertexInfo = VertexCache.getInstance().getVertex(vinfo, numberOfVertex, context.bone_uploaded_matrix, numberOfWeightsForBuffer);
            	vertexCacheLookupStatistics.end();
            }

            ByteBuffer byteBuffer = bufferManager.getBuffer(bufferId);
            byteBuffer.clear();
            FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
            floatBuffer.clear();

    		if (!useVertexCache && re.isVertexArrayAvailable()) {
				re.bindVertexArray(0);
    		}

            switch (type) {
                case PRIM_POINT:
                case PRIM_LINE:
                case PRIM_LINES_STRIPS:
                case PRIM_TRIANGLE:
                case PRIM_TRIANGLE_STRIPS:
                case PRIM_TRIANGLE_FANS:
                    re.setVertexInfo(vinfo, false, useVertexColor, useTexture, type);

                    float[] normalizedNormal = new float[3];
                    if (cachedVertexInfo == null) {
                		vertexReadingStatistics.start();
                        for (int i = 0; i < numberOfVertex; i++) {
                            int addr = vinfo.getAddress(mem, i);

                            VertexState v = vinfo.readVertex(mem, addr);

                            // Do skinning first as it modifies v.p and v.n
                            if (mustComputeWeights && vinfo.position != 0) {
                                doSkinning(vinfo, v);
                            }

                            if (useTextureFromNormal) {
                                floatBuffer.put(v.n, 0, 3);
                            } else if (useTextureFromNormalizedNormal) {
                            	float normalLength = (float) Math.sqrt(v.n[0] * v.n[0] + v.n[1] * v.n[1] + v.n[2] * v.n[2]);
                            	normalizedNormal[0] = v.n[0] / normalLength;
                            	normalizedNormal[1] = v.n[1] / normalLength;
                            	normalizedNormal[2] = v.n[2] / normalLength;
                                floatBuffer.put(normalizedNormal, 0, 3);
                            } else if (useTextureFromPosition) {
                                floatBuffer.put(v.p, 0, 3);
                            } else if (useTexture || vinfo.texture != 0) {
                                floatBuffer.put(v.t);
                            }
                            if (useVertexColor) {
                                floatBuffer.put(v.c);
                            }
                            if (vinfo.normal != 0) {
                                floatBuffer.put(v.n);
                            }
                            if (vinfo.position != 0) {
                                floatBuffer.put(v.p);
                            }
                            if (numberOfWeightsForBuffer > 0) {
                                floatBuffer.put(v.boneWeights, 0, numberOfWeightsForBuffer);
                            }

                            if (isLogTraceEnabled) {
                                if (vinfo.texture != 0 && vinfo.position != 0) {
                                    log.trace("  vertex#" + i + " (" + ((int) v.t[0]) + "," + ((int) v.t[1]) + ") at (" + ((int) v.p[0]) + "," + ((int) v.p[1]) + "," + ((int) v.p[2]) + ")");
                                }
                            }
                        }
                    	vertexReadingStatistics.end();

                        if (useVertexCache) {
                            cachedVertexInfo = new VertexInfo(vinfo);
                            VertexCache.getInstance().addVertex(re, cachedVertexInfo, numberOfVertex, context.bone_uploaded_matrix, numberOfWeightsForBuffer);
                            int size = floatBuffer.position();
                            floatBuffer.rewind();
                            needSetDataPointers = cachedVertexInfo.loadVertex(re, floatBuffer, size);
                        } else {
                            bufferManager.setBufferData(bufferId, floatBuffer.position() * SIZEOF_FLOAT, byteBuffer, IRenderingEngine.RE_STREAM_DRAW);
                        }
                    } else {
                        if (isLogDebugEnabled) {
                            log.debug("Reusing cached Vertex Data");
                        }
                        needSetDataPointers = cachedVertexInfo.bindVertex(re);
                    }
                    if (needSetDataPointers) {
                    	setDataPointers(nVertex, useVertexColor, nColor, useTexture, nTexCoord, vinfo.normal != 0, numberOfWeightsForBuffer, cachedVertexInfo == null);
                    }
                	drawArraysStatistics.start();
                    re.drawArrays(type, 0, numberOfVertex);
                	drawArraysStatistics.end();
                    maxSpriteHeight = Integer.MAX_VALUE;
                    maxSpriteWidth = Integer.MAX_VALUE;
                    break;

                case PRIM_SPRITES:
                    re.setVertexInfo(vinfo, false, useVertexColor, useTexture, IRenderingEngine.RE_QUADS);
                	re.disableFlag(IRenderingEngine.GU_CULL_FACE);

                	float[] mvpMatrix = null;
                	if (!vinfo.transform2D) {
                		mvpMatrix = new float[4 * 4];
                		// pre-Compute the MVP (Model-View-Projection) matrix
                		matrixMult(mvpMatrix, context.model_uploaded_matrix, context.view_uploaded_matrix);
                		matrixMult(mvpMatrix, mvpMatrix, getProjectionMatrix());
                	}

                	if (cachedVertexInfo == null) {
            			vertexReadingStatistics.start();
                        for (int i = 0; i < numberOfVertex; i += 2) {
                            int addr1 = vinfo.getAddress(mem, i);
                            int addr2 = vinfo.getAddress(mem, i + 1);
                            VertexState v1 = vinfo.readVertex(mem, addr1);
                            VertexState v2 = vinfo.readVertex(mem, addr2);

                            v1.p[2] = v2.p[2];

                            if (v2.p[1] > maxSpriteHeight) {
                                maxSpriteHeight = (int) v2.p[1];
                            }
                            if (v2.p[1] > maxSpriteWidth) {
                            	maxSpriteWidth = (int) v2.p[1];
                            }

                            //
                            // Texture flip tested using the GElist application:
                            // - it depends on the X and Y coordinates:
                            //   GU_TRANSFORM_3D:
                            //     X1 < X2 && Y1 < Y2 :     flipped
                            //     X1 > X2 && Y1 > Y2 :     flipped
                            //     X1 < X2 && Y1 > Y2 : not flipped
                            //     X1 > X2 && Y1 < Y2 : not flipped
                            //   GU_TRANSFORM_2D: opposite results because
                            //                    the Y-Axis is upside-down in 2D
                            //     X1 < X2 && Y1 < Y2 : not flipped
                            //     X1 > X2 && Y1 > Y2 : not flipped
                            //     X1 < X2 && Y1 > Y2 :     flipped
                            //     X1 > X2 && Y1 < Y2 :     flipped
                            // - the tests for GU_TRANSFORM_3D are based on the coordinates
                            //   after the MVP (Model-View-Projection) transformation
                            // - texture coordinates are irrelevant
                            //
                            float x1, y1, x2, y2;
                            if (mvpMatrix == null) {
                            	x1 =  v1.p[0];
                            	y1 = -v1.p[1]; // Y-Axis is upside-down in 2D
                            	x2 =  v2.p[0];
                            	y2 = -v2.p[1]; // Y-Axis is upside-down in 2D
                            } else {
                            	// Apply the MVP transformation to both position coordinates
                            	float[] mvpPosition = new float[2];
                            	vectorMult(mvpPosition, mvpMatrix, v1.p);
                            	x1 = mvpPosition[0];
                            	y1 = mvpPosition[1];
                            	vectorMult(mvpPosition, mvpMatrix, v2.p);
                            	x2 = mvpPosition[0];
                            	y2 = mvpPosition[1];
                            }
                            boolean flippedTexture = (y1 < y2 && x1 < x2) ||
                                                     (y1 > y2 && x1 > x2);

                            if (isLogDebugEnabled) {
                            	log(String.format("  sprite (%.0f,%.0f)-(%.0f,%.0f) at (%.0f,%.0f,%.0f)-(%.0f,%.0f,%.0f)%s", v1.t[0], v1.t[1], v2.t[0], v2.t[1], v1.p[0], v1.p[1], v1.p[2], v2.p[0], v2.p[1], v2.p[2], flippedTexture ? " flipped" : ""));
                            }

                            // V1
                            if (vinfo.texture != 0) {
                                floatBuffer.put(v1.t);
                            }
                            if (useVertexColor) {
                                floatBuffer.put(v2.c);
                            }
                            if (vinfo.normal != 0) {
                                floatBuffer.put(v2.n);
                            }
                            if (vinfo.position != 0) {
                                floatBuffer.put(v1.p);
                            }

                            if (vinfo.texture != 0) {
                                if (flippedTexture) {
                                    floatBuffer.put(v2.t[0]).put(v1.t[1]);
                                } else {
                                    floatBuffer.put(v1.t[0]).put(v2.t[1]);
                                }
                            }
                            if (useVertexColor) {
                                floatBuffer.put(v2.c);
                            }
                            if (vinfo.normal != 0) {
                                floatBuffer.put(v2.n);
                            }
                            if (vinfo.position != 0) {
                                floatBuffer.put(v1.p[0]).put(v2.p[1]).put(v2.p[2]);
                            }

                            // V2
                            if (vinfo.texture != 0) {
                                floatBuffer.put(v2.t);
                            }
                            if (useVertexColor) {
                                floatBuffer.put(v2.c);
                            }
                            if (vinfo.normal != 0) {
                                floatBuffer.put(v2.n);
                            }
                            if (vinfo.position != 0) {
                                floatBuffer.put(v2.p);
                            }

                            if (vinfo.texture != 0) {
                                if (flippedTexture) {
                                    floatBuffer.put(v1.t[0]).put(v2.t[1]);
                                } else {
                                    floatBuffer.put(v2.t[0]).put(v1.t[1]);
                                }
                            }
                            if (useVertexColor) {
                                floatBuffer.put(v2.c);
                            }
                            if (vinfo.normal != 0) {
                                floatBuffer.put(v2.n);
                            }
                            if (vinfo.position != 0) {
                                floatBuffer.put(v2.p[0]).put(v1.p[1]).put(v2.p[2]);
                            }
                        }
                    	vertexReadingStatistics.end();

                        if (useVertexCache) {
                            cachedVertexInfo = new VertexInfo(vinfo);
                            VertexCache.getInstance().addVertex(re, cachedVertexInfo, numberOfVertex, context.bone_uploaded_matrix, numberOfWeightsForBuffer);
                            int size = floatBuffer.position();
                            floatBuffer.rewind();
                            needSetDataPointers = cachedVertexInfo.loadVertex(re, floatBuffer, size);
                        } else {
                            bufferManager.setBufferData(bufferId, floatBuffer.position() * SIZEOF_FLOAT, byteBuffer, IRenderingEngine.RE_STREAM_DRAW);
                        }
                    } else {
                        if (isLogDebugEnabled) {
                            log.debug("Reusing cached Vertex Data");
                        }
                        needSetDataPointers = cachedVertexInfo.bindVertex(re);
                    }
                	if (needSetDataPointers) {
                		setDataPointers(nVertex, useVertexColor, nColor, useTexture, nTexCoord, vinfo.normal != 0, 0, cachedVertexInfo == null);
                	}
            		drawArraysStatistics.start();
                    re.drawArrays(IRenderingEngine.RE_QUADS, 0, numberOfVertex * 2);
                	drawArraysStatistics.end();
                    context.cullFaceFlag.updateEnabled();
                    break;
            }
        }

    	vertexStatistics.end();

        // Don't capture the ram if the vertex list is embedded in the display list. TODO handle stall_addr == 0 better
        // TODO may need to move inside the loop if indices are used, or find the largest index so we can calculate the size of the vertex list
        if (State.captureGeNextFrame) {
    		if (!isVertexBufferEmbedded()) {
    				log.info("Capture PRIM");
	            CaptureManager.captureRAM(vinfo.ptr_vertex, vinfo.vertexSize * numberOfVertex);
    		}
            display.captureGeImage();
            textureChanged = true;
        }

        endRendering(useVertexColor, useTexture, numberOfVertex);
    }

    private void executeCommandTRXKICK() {
    	context.textureTx_pixelSize = normalArgument & 0x1;

    	context.textureTx_sourceAddress &= Memory.addressMask;
    	context.textureTx_destinationAddress &= Memory.addressMask;

        if (isLogDebugEnabled) {
            log(String.format("%s from 0x%08X(%d,%d) to 0x%08X(%d,%d), width=%d, height=%d, pixelSize=%d",
                              helper.getCommandString(TRXKICK),
                              context.textureTx_sourceAddress,
                              context.textureTx_sx,
                              context.textureTx_sy,
                              context.textureTx_destinationAddress,
                              context.textureTx_dx,
                              context.textureTx_dy,
                              context.textureTx_width,
                              context.textureTx_height,
                              context.textureTx_pixelSize));
        }

        if (!Memory.isAddressGood(context.textureTx_sourceAddress)) {
        	error(String.format("%s invalid source address 0x%08X", helper.getCommandString(TRXKICK), context.textureTx_sourceAddress));
        	return;
        }
        if (!Memory.isAddressGood(context.textureTx_destinationAddress)) {
        	error(String.format("%s invalid destination address 0x%08X", helper.getCommandString(TRXKICK), context.textureTx_destinationAddress));
        	return;
        }

        usingTRXKICK = true;
        updateGeBuf();

        int pixelFormatGe = context.psm;
        int bpp = (context.textureTx_pixelSize == TRXKICK_16BIT_TEXEL_SIZE) ? 2 : 4;
        int bppGe = sceDisplay.getPixelFormatBytes(pixelFormatGe);

        memoryForGEUpdated();

        if (!display.isGeAddress(context.textureTx_destinationAddress) || bpp != bppGe) {
            if (isLogDebugEnabled) {
                if (bpp != bppGe) {
                    log(helper.getCommandString(TRXKICK) + " BPP not compatible with GE");
                } else {
                    log(helper.getCommandString(TRXKICK) + " not in Ge Address space");
                }
            }

            // Remove the destination address from the texture cache
            if (canCacheTexture(context.textureTx_destinationAddress)) {
	            TextureCache textureCache = TextureCache.getInstance();
	            textureCache.resetTextureAlreadyHashed(context.textureTx_destinationAddress, context.tex_clut_addr);
	            textureCache.resetTextureAlreadyHashed(context.textureTx_destinationAddress, 0);
            }
            if (context.textureTx_destinationAddress == (context.texture_base_pointer[0] & Memory.addressMask)) {
            	textureChanged = true;
            }

            int width = context.textureTx_width;
            int height = context.textureTx_height;

            int srcAddress = context.textureTx_sourceAddress + (context.textureTx_sy * context.textureTx_sourceLineWidth + context.textureTx_sx) * bpp;
            int dstAddress = context.textureTx_destinationAddress + (context.textureTx_dy * context.textureTx_destinationLineWidth + context.textureTx_dx) * bpp;
            Memory memory = Memory.getInstance();
            if (context.textureTx_sourceLineWidth == width && context.textureTx_destinationLineWidth == width) {
                // All the lines are adjacent in memory,
                // copy them all in a single memcpy operation.
                int copyLength = height * width * bpp;
                if (isLogDebugEnabled) {
                    log(String.format("%s memcpy(0x%08X-0x%08X, 0x%08X, 0x%X)", helper.getCommandString(TRXKICK), dstAddress, dstAddress + copyLength, srcAddress, copyLength));
                }
                memory.memcpy(dstAddress, srcAddress, copyLength);
            } else {
                // The lines are not adjacent in memory: copy line by line.
                int copyLength = width * bpp;
                int srcLineLength = context.textureTx_sourceLineWidth * bpp;
                int dstLineLength = context.textureTx_destinationLineWidth * bpp;
                for (int y = 0; y < height; y++) {
                    if (isLogDebugEnabled) {
                        log(String.format("%s memcpy(0x%08X-0x%08X, 0x%08X, 0x%X)", helper.getCommandString(TRXKICK), dstAddress, dstAddress + copyLength, srcAddress, copyLength));
                    }
                    memory.memcpy(dstAddress, srcAddress, copyLength);
                    srcAddress += srcLineLength;
                    dstAddress += dstLineLength;
                }
            }

            if (State.captureGeNextFrame) {
                log.warn("TRXKICK outside of Ge Address space not supported in capture yet");
            }
        } else {
            int width = context.textureTx_width;
            int height = context.textureTx_height;
            int dx = context.textureTx_dx;
            int dy = context.textureTx_dy;
            int lineWidth = context.textureTx_sourceLineWidth;

            int geAddr = display.getTopAddrGe();
            dy += (context.textureTx_destinationAddress - geAddr) / (display.getBufferWidthGe() * bpp);
            dx += ((context.textureTx_destinationAddress - geAddr) % (display.getBufferWidthGe() * bpp)) / bpp;

            if (isLogDebugEnabled) {
                log(helper.getCommandString(TRXKICK) + " in Ge Address space: dx=" + dx + ", dy=" + dy + ", width=" + width + ", height=" + height + ", lineWidth=" + lineWidth + ", bpp=" + bpp);
            }

            int texture = re.genTexture();
            re.bindTexture(texture);
			re.setTextureFormat(pixelFormatGe, false);

            re.startDirectRendering(true, false, true, true, false, 480, 272);
            re.setPixelStore(lineWidth, bpp);

            int textureSize = lineWidth * height * bpp;
            Buffer buffer = Memory.getInstance().getBuffer(context.textureTx_sourceAddress, textureSize);

            if (State.captureGeNextFrame) {
                log.info("Capture TRXKICK");
                CaptureManager.captureRAM(context.textureTx_sourceAddress, lineWidth * height * bpp);
            }

            //
            // glTexImage2D only supports
            //		width = (1 << n)	for some integer n
            //		height = (1 << m)	for some integer m
            //
            // This the reason why we are also using glTexSubImage2D.
            //
            int bufferHeight = Utilities.makePow2(height);
            re.setTexImage(0,pixelFormatGe, lineWidth, bufferHeight, pixelFormatGe, pixelFormatGe, 0, null);
            re.setTexSubImage(0, context.textureTx_sx, context.textureTx_sy, width, height, pixelFormatGe, pixelFormatGe, textureSize, buffer);

            re.beginDraw(PRIM_SPRITES);
            re.drawColor(1.0f, 1.0f, 1.0f, 1.0f);

            float texCoordX = width / (float) lineWidth;
            float texCoordY = height / (float) bufferHeight;

            re.drawTexCoord(0.0f, 0.0f);
            re.drawVertex(dx, dy);

            re.drawTexCoord(texCoordX, 0.0f);
            re.drawVertex(dx + width, dy);

            re.drawTexCoord(texCoordX, texCoordY);
            re.drawVertex(dx + width, dy + height);

            re.drawTexCoord(0.0f, texCoordY);
            re.drawVertex(dx, dy + height);

            re.endDraw();

            re.endDirectRendering();
            re.deleteTexture(texture);
        }
    }

    private void executeCommandBBOX() {
        Memory mem = Memory.getInstance();
        int numberOfVertexBoundingBox = normalArgument & 0xFF;

        if (vinfo.ptr_vertex == 0) {
        	// The GE is initialized with a NULL vertex address, do not log an error
        	if (isLogDebugEnabled) {
                log.debug(String.format("%s null vertex address", helper.getCommandString(BBOX)));
        	}
        	return;
        } else if (!Memory.isAddressGood(vinfo.ptr_vertex)) {
            // Abort here to avoid a lot of useless memory read errors...
            error(String.format("%s Invalid vertex address 0x%08X", helper.getCommandString(BBOX), vinfo.ptr_vertex));
            return;
        } else if (vinfo.position == 0) {
            log.warn(helper.getCommandString(BBOX) + " no positions for vertex!");
            return;
        } else if (!re.hasBoundingBox()) {
        	if (!bboxWarningDisplayed) {
        		log.warn("Not supported by your OpenGL version (but can be ignored): " + helper.getCommandString(BBOX) + " numberOfVertex=" + numberOfVertexBoundingBox);
        		bboxWarningDisplayed = true;
        	}
            return;
        } else if ((numberOfVertexBoundingBox % 8) != 0) {
            // How to interpret non-multiple of 8?
            log.warn(helper.getCommandString(BBOX) + " unsupported numberOfVertex=" + numberOfVertexBoundingBox);
        } else if (isLogDebugEnabled) {
            log.debug(helper.getCommandString(BBOX) + " numberOfVertex=" + numberOfVertexBoundingBox);
        }

        boolean useVertexColor = initRendering();

        re.beginBoundingBox(numberOfVertexBoundingBox);
        for (int i = 0; i < numberOfVertexBoundingBox; i++) {
            int addr = vinfo.getAddress(mem, i);

            VertexState v = vinfo.readVertex(mem, addr);
            if (isLogDebugEnabled) {
                log.debug(String.format("%s (%f,%f,%f)", helper.getCommandString(BBOX), v.p[0], v.p[1], v.p[2]));
            }

            int vertexIndex = i % 8;
            bboxVertices[vertexIndex][0] = v.p[0];
            bboxVertices[vertexIndex][1] = v.p[1];
            bboxVertices[vertexIndex][2] = v.p[2];

            if (vertexIndex == 7) {
            	re.drawBoundingBox(bboxVertices);
            }
        }
        re.endBoundingBox(vinfo);

        endRendering(useVertexColor, false, numberOfVertexBoundingBox);
    }

    private void executeCommandBJUMP() {
        boolean takeConditionalJump = false;

        if (re.hasBoundingBox()) {
        	takeConditionalJump = !re.isBoundingBoxVisible();
        }

        if (takeConditionalJump) {
            int oldPc = currentList.getPc();
            currentList.jumpRelativeOffset(normalArgument);
            int newPc = currentList.getPc();
            if (isLogDebugEnabled) {
                log(String.format("%s old PC: 0x%08X, new PC: 0x%08X", helper.getCommandString(BJUMP), oldPc, newPc));
            }
        } else {
            if (isLogDebugEnabled) {
                log(String.format("%s not taking Conditional Jump", helper.getCommandString(BJUMP)));
            }
        }
    }

    private void executeCommandBONE() {
        // Multiple BONE matrix can be loaded in sequence
        // without having to issue a BOFS for each matrix.
        int matrixIndex = boneMatrixIndex / 12;
        int elementIndex = boneMatrixIndex % 12;
        if (matrixIndex >= 8) {
        	if(isLogDebugEnabled)
        		log("Ignoring BONE matrix element: boneMatrixIndex=" + boneMatrixIndex);
        } else {
            float floatArgument = floatArgument(normalArgument);
        	context.bone_uploaded_matrix[matrixIndex][elementIndex] = floatArgument;
        	context.boneMatrixLinear[(boneMatrixIndex / 3) * 4 + (boneMatrixIndex % 3)] = floatArgument;
            if (matrixIndex >= boneMatrixLinearUpdatedMatrix) {
                boneMatrixLinearUpdatedMatrix = matrixIndex + 1;
            }

            boneMatrixIndex++;

            if (isLogDebugEnabled && (boneMatrixIndex % 12) == 0) {
                for (int x = 0; x < 3; x++) {
                    log.debug(String.format("bone matrix %d %.2f %.2f %.2f %.2f",
                            matrixIndex,
                            context.bone_uploaded_matrix[matrixIndex][x + 0],
                            context.bone_uploaded_matrix[matrixIndex][x + 3],
                            context.bone_uploaded_matrix[matrixIndex][x + 6],
                            context.bone_uploaded_matrix[matrixIndex][x + 9]));
                }
            }
        }
    }

    private void executeCommandNOP() {
        if (isLogDebugEnabled) {
            log(helper.getCommandString(NOP));
        }

        // Check if we are not reading from an invalid memory region.
        // Abort the list if this is the case.
        // This is only done in the NOP command to not impact performance.
        checkCurrentListPc();
    }

    private void executeCommandVADDR() {
        vinfo.ptr_vertex = currentList.getAddressRelOffset(normalArgument);
        if (isLogDebugEnabled) {
            log(helper.getCommandString(VADDR) + " " + String.format("%08x", vinfo.ptr_vertex));
        }
    }

    private void executeCommandIADDR() {
        vinfo.ptr_index = currentList.getAddressRelOffset(normalArgument);
        if (isLogDebugEnabled) {
            log(helper.getCommandString(IADDR) + " " + String.format("%08x", vinfo.ptr_index));
        }
    }

    private void executeCommandBEZIER() {
        int ucount = normalArgument & 0xFF;
        int vcount = (normalArgument >> 8) & 0xFF;
        if (isLogDebugEnabled) {
            log(helper.getCommandString(BEZIER) + " ucount=" + ucount + ", vcount=" + vcount);
        }

        updateGeBuf();
        somethingDisplayed = true;
        loadTexture();

        drawBezier(ucount, vcount);
    }

    private void executeCommandSPLINE() {
        // Number of control points.
        int sp_ucount = normalArgument & 0xFF;
        int sp_vcount = (normalArgument >> 8) & 0xFF;
        // Knot types.
        int sp_utype = (normalArgument >> 16) & 0x3;
        int sp_vtype = (normalArgument >> 18) & 0x3;

        if (isLogDebugEnabled) {
            log(helper.getCommandString(SPLINE) + " sp_ucount=" + sp_ucount + ", sp_vcount=" + sp_vcount +
                    " sp_utype=" + sp_utype + ", sp_vtype=" + sp_vtype);
        }

        updateGeBuf();
        somethingDisplayed = true;
        loadTexture();

        drawSpline(sp_ucount, sp_vcount, sp_utype, sp_vtype);
    }

    private void executeCommandJUMP() {
        int oldPc = currentList.getPc();
        currentList.jumpRelativeOffset(normalArgument);
        int newPc = currentList.getPc();
        if (isLogDebugEnabled) {
            log(String.format("%s old PC: 0x%08X, new PC: 0x%08X", helper.getCommandString(JUMP), oldPc, newPc));
        }
    }

    private void executeCommandCALL() {
        int oldPc = currentList.getPc();
        currentList.callRelativeOffset(normalArgument);
        int newPc = currentList.getPc();
        if (isLogDebugEnabled) {
            log(String.format("%s old PC: 0x%08X, new PC: 0x%08X", helper.getCommandString(CALL), oldPc, newPc));
        }
    }

    private void executeCommandRET() {
        int oldPc = currentList.getPc();
        currentList.ret();
        int newPc = currentList.getPc();
        if (isLogDebugEnabled) {
            log(String.format("%s old PC: 0x%08X, new PC: 0x%08X", helper.getCommandString(RET), oldPc, newPc));
        }
    }

    private void executeCommandEND() {
        // Try to end the current list.
        // The list only ends (isEnded() == true) if FINISH was called previously.
        // In SIGNAL + END cases, isEnded() still remains false.
        currentList.endList();
        currentList.pauseList();
        if (isLogDebugEnabled) {
            log(helper.getCommandString(END) + " pc=0x" + Integer.toHexString(currentList.getPc()));
        }
        updateGeBuf();
    }

    private void executeCommandSIGNAL() {
        int behavior = (normalArgument >> 16) & 0xFF;
        int signal = normalArgument & 0xFFFF;
        if (isLogDebugEnabled) {
        	log(String.format("%s (behavior=%d, signal=0x%X)", helper.getCommandString(SIGNAL), behavior, signal));
        }

        switch (behavior) {
        	case sceGe_user.PSP_GE_SIGNAL_SYNC: {
	        	// Skip END / FINISH / END
	        	Memory mem = Memory.getInstance();
	        	if (command(mem.read32(currentList.getPc())) == END) {
	        		currentList.readNextInstruction();
	        		if (command(mem.read32(currentList.getPc())) == FINISH) {
	            		currentList.readNextInstruction();
	                	if (command(mem.read32(currentList.getPc())) == END) {
	                		currentList.readNextInstruction();
	                	}
	        		}
	        	}
                if (isLogDebugEnabled) {
                    log(String.format("PSP_GE_SIGNAL_SYNC ignored PC: 0x%08X", currentList.getPc()));
                }
	        	break;
        	}
        	case sceGe_user.PSP_GE_SIGNAL_CALL: {
	            // Call list using absolute address from SIGNAL + END.
	            Memory mem = Memory.getInstance();
	        	if (command(mem.read32(currentList.getPc())) == END) {
	                int hi16 = signal & 0x0FFF;
	                // Read & skip END
	        		int lo16 = (currentList.readNextInstruction() & 0xFFFF);
	                int addr = (hi16 << 16) | lo16;
	                int oldPc = currentList.getPc();
	                currentList.callAbsolute(addr);
	                int newPc = currentList.getPc();
	                if (isLogDebugEnabled) {
	                    log(String.format("PSP_GE_SIGNAL_CALL old PC: 0x%08X, new PC: 0x%08X", oldPc, newPc));
	                }
	            }
	            break;
        	}
        	case sceGe_user.PSP_GE_SIGNAL_RETURN: {
	            // Return from PSP_GE_SIGNAL_CALL.
	            Memory mem = Memory.getInstance();
	        	if (command(mem.read32(currentList.getPc())) == END) {
	        		// Skip END
	        		currentList.readNextInstruction();
	                int oldPc = currentList.getPc();
	                currentList.ret();
	                int newPc = currentList.getPc();
	                if (isLogDebugEnabled) {
	                    log(String.format("PSP_GE_SIGNAL_RETURN old PC: 0x%08X, new PC: 0x%08X", oldPc, newPc));
	                }
	            }
	        	break;
        	}
        	case sceGe_user.PSP_GE_SIGNAL_TBP0_REL:
        	case sceGe_user.PSP_GE_SIGNAL_TBP1_REL:
        	case sceGe_user.PSP_GE_SIGNAL_TBP2_REL:
        	case sceGe_user.PSP_GE_SIGNAL_TBP3_REL:
        	case sceGe_user.PSP_GE_SIGNAL_TBP4_REL:
        	case sceGe_user.PSP_GE_SIGNAL_TBP5_REL:
        	case sceGe_user.PSP_GE_SIGNAL_TBP6_REL:
        	case sceGe_user.PSP_GE_SIGNAL_TBP7_REL: {
                // Overwrite TBPn and TBPw with SIGNAL + END (uses relative address only).
                Memory mem = Memory.getInstance();
            	if (command(mem.read32(currentList.getPc())) == END) {
                    int hi16 = signal & 0xFFFF;
                    // Read & skip END
                    int ins = currentList.readNextInstruction();
            		int lo16 = ins & 0xFFFF;
                    int width = (ins >> 16) & 0xFF;
                    int addr = currentList.getAddressRel((hi16 << 16) | lo16);
                    context.texture_base_pointer[behavior - sceGe_user.PSP_GE_SIGNAL_TBP0_REL] = addr;
                    context.texture_buffer_width[behavior - sceGe_user.PSP_GE_SIGNAL_TBP0_REL] = width;
                }
        		break;
        	}
        	case sceGe_user.PSP_GE_SIGNAL_TBP0_REL_OFFSET:
        	case sceGe_user.PSP_GE_SIGNAL_TBP1_REL_OFFSET:
        	case sceGe_user.PSP_GE_SIGNAL_TBP2_REL_OFFSET:
        	case sceGe_user.PSP_GE_SIGNAL_TBP3_REL_OFFSET:
        	case sceGe_user.PSP_GE_SIGNAL_TBP4_REL_OFFSET:
        	case sceGe_user.PSP_GE_SIGNAL_TBP5_REL_OFFSET:
        	case sceGe_user.PSP_GE_SIGNAL_TBP6_REL_OFFSET:
        	case sceGe_user.PSP_GE_SIGNAL_TBP7_REL_OFFSET: {
                // Overwrite TBPn and TBPw with SIGNAL + END (uses relative address with offset).
                Memory mem = Memory.getInstance();
            	if (command(mem.read32(currentList.getPc())) == END) {
                    int hi16 = signal & 0xFFFF;
                    // Read & skip END
                    int ins = currentList.readNextInstruction();
            		int lo16 = ins & 0xFFFF;
                    int width = (ins >> 16) & 0xFF;
                    int addr = currentList.getAddressRelOffset((hi16 << 16) | lo16);
                    context.texture_base_pointer[behavior - sceGe_user.PSP_GE_SIGNAL_TBP0_REL_OFFSET] = addr;
                    context.texture_buffer_width[behavior - sceGe_user.PSP_GE_SIGNAL_TBP7_REL_OFFSET] = width;
                }
        		break;
        	}
        	case sceGe_user.PSP_GE_SIGNAL_HANDLER_SUSPEND:
        	case sceGe_user.PSP_GE_SIGNAL_HANDLER_CONTINUE:
        	case sceGe_user.PSP_GE_SIGNAL_HANDLER_PAUSE: {
            	currentList.clearRestart();
            	currentList.pushSignalCallback(currentList.id, behavior, signal);
            	break;
        	}
        	default: {
                if (isLogWarnEnabled) {
                    log.warn(String.format("%s (behavior=%d, signal=0x%X) unknown behavior at 0x%08X", helper.getCommandString(SIGNAL), behavior, signal, currentList.getPc() - 4));
                }
        	}
        }
    }

    private void executeCommandFINISH() {
        if (isLogDebugEnabled) {
            log(helper.getCommandString(FINISH) + " " + getArgumentLog(normalArgument));
        }
        currentList.clearRestart();
        currentList.finishList();
        currentList.pushFinishCallback(currentList.id, normalArgument);
    }

    private void executeCommandBASE() {
    	context.base = (normalArgument << 8) & 0xff000000;
        // Bits of (normalArgument & 0x0000FFFF) are ignored
        // (tested: "Ape Escape On the Loose")
        if (isLogDebugEnabled) {
            log(helper.getCommandString(BASE) + " " + String.format("%08x", context.base));
        }
    }

    private void executeCommandVTYPE() {
        int old_transform_mode = context.transform_mode;
        boolean old_vertex_hasColor = vinfo.color != 0;
        vinfo.processType(normalArgument);
        context.transform_mode = (normalArgument >> 23) & 0x1;
        boolean vertex_hasColor = vinfo.color != 0;

        //Switching from 2D to 3D or 3D to 2D?
        if (old_transform_mode != context.transform_mode) {
            projectionMatrixUpload.setChanged(true);
            modelMatrixUpload.setChanged(true);
            viewMatrixUpload.setChanged(true);
            textureMatrixUpload.setChanged(true);
            viewportChanged = true;
            depthChanged = true;
            materialChanged = true;
            // Switching from 2D to 3D?
            if (context.transform_mode == VTYPE_TRANSFORM_PIPELINE_TRANS_COORD) {
                lightingChanged = true;
            }
        } else if (old_vertex_hasColor != vertex_hasColor) {
            // Materials have to be reloaded when the vertex color presence is changing
            materialChanged = true;
        }

        if (isLogDebugEnabled) {
            log(helper.getCommandString(VTYPE) + " " + vinfo.toString());
        }
    }

    private void executeCommandOFFSET_ADDR() {
    	context.baseOffset = normalArgument << 8;
        if (isLogDebugEnabled) {
            log(String.format("%s 0x%08X", helper.getCommandString(OFFSET_ADDR), context.baseOffset));
        }
    }

    private void executeCommandORIGIN_ADDR() {
    	context.baseOffset = currentList.getPc() - 4;
        if (normalArgument != 0) {
            log.warn(String.format("%s unknown argument 0x%08X", helper.getCommandString(ORIGIN_ADDR), normalArgument));
        } else if (isLogDebugEnabled) {
            log(String.format("%s 0x%08X originAddr=0x%08X", helper.getCommandString(ORIGIN_ADDR), normalArgument, context.baseOffset));
        }
    }

    private void executeCommandREGION1() {
    	int old_region_x1 = context.region_x1;
    	int old_region_y1 = context.region_y1;
    	context.region_x1 = normalArgument & 0x3ff;
    	context.region_y1 = (normalArgument >> 10) & 0x3ff;
    	if (old_region_x1 != context.region_x1 || old_region_y1 != context.region_y1) {
    		scissorChanged = true;
    	}
    }

    private void executeCommandREGION2() {
    	int old_region_x2 = context.region_x2;
    	int old_region_y2 = context.region_y2;
    	context.region_x2 = normalArgument & 0x3ff;
    	context.region_y2 = (normalArgument >> 10) & 0x3ff;
    	context.region_width = (context.region_x2 + 1) - context.region_x1;
    	context.region_height = (context.region_y2 + 1) - context.region_y1;
        if (isLogDebugEnabled) {
            log("drawRegion(" + context.region_x1 + "," + context.region_y1 + "," + context.region_width + "," + context.region_height + ")");
        }
    	if (old_region_x2 != context.region_x2 || old_region_y2 != context.region_y2) {
    		scissorChanged = true;
    	}
    }

    private void executeCommandLTE() {
    	context.lightingFlag.setEnabled(normalArgument);
        if (context.lightingFlag.isEnabled()) {
            lightingChanged = true;
            materialChanged = true;
        }
    }

    private void executeCommandLTEn() {
        int lnum = command - LTE0;
        EnableDisableFlag lightFlag = context.lightFlags[lnum];
        lightFlag.setEnabled(normalArgument);
        if (lightFlag.isEnabled()) {
            lightingChanged = true;
        }
    }

    private void executeCommandCPE() {
    	context.clipPlanesFlag.setEnabled(normalArgument);
    }

    private void executeCommandBCE() {
    	context.cullFaceFlag.setEnabled(normalArgument);
    }

    private void executeCommandTME() {
    	context.textureFlag.setEnabled(normalArgument);
    }

    private void executeCommandFGE() {
    	context.fogFlag.setEnabled(normalArgument);
        if (context.fogFlag.isEnabled()) {
        	re.setFogHint();
        }
    }

    private void executeCommandDTE() {
    	context.ditherFlag.setEnabled(normalArgument);
    }

    private void executeCommandABE() {
    	context.blendFlag.setEnabled(normalArgument);
    }

    private void executeCommandATE() {
    	context.alphaTestFlag.setEnabled(normalArgument);
    }

    private void executeCommandZTE() {
    	context.depthTestFlag.setEnabled(normalArgument);
        if (context.depthTestFlag.isEnabled()) {
            // OpenGL requires the Depth parameters to be reloaded
            depthChanged = true;
        }
    }

    private void executeCommandSTE() {
    	context.stencilTestFlag.setEnabled(normalArgument);
    }

    private void executeCommandAAE() {
    	context.lineSmoothFlag.setEnabled(normalArgument);
        if (context.lineSmoothFlag.isEnabled()) {
        	re.setLineSmoothHint();
        }
    }

    private void executeCommandPCE() {
    	context.patchCullFaceFlag.setEnabled(normalArgument);
    }

    private void executeCommandCTE() {
    	context.colorTestFlag.setEnabled(normalArgument);
    }

    private void executeCommandLOE() {
    	context.colorLogicOpFlag.setEnabled(normalArgument);
    }

    private void executeCommandBOFS() {
        boneMatrixIndex = normalArgument;
        if (isLogDebugEnabled) {
            log("bone matrix offset", normalArgument);
        }
    }

    private void executeCommandMWn() {
        int index = command - MW0;
        float floatArgument = floatArgument(normalArgument);
        context.morph_weight[index] = floatArgument;
        re.setMorphWeight(index, floatArgument);
        if (isLogDebugEnabled) {
            log("morph weight " + index, floatArgument);
        }
    }

    private void executeCommandPSUB() {
    	context.patch_div_s = normalArgument & 0xFF;
    	context.patch_div_t = (normalArgument >> 8) & 0xFF;
        re.setPatchDiv(context.patch_div_s, context.patch_div_t);
        if (isLogDebugEnabled) {
            log(helper.getCommandString(PSUB) + " patch_div_s=" + context.patch_div_s + ", patch_div_t=" + context.patch_div_t);
        }
    }

    private void executeCommandPPRIM() {
    	context.patch_prim = (normalArgument & 0x3);
        // Primitive type to use in patch division:
        // 0 - Triangle.
        // 1 - Line.
        // 2 - Point.
        re.setPatchPrim(context.patch_prim);
        if (isLogDebugEnabled) {
            log(helper.getCommandString(PPRIM) + " patch_prim=" + context.patch_prim);
        }
    }

    private void executeCommandPFACE() {
        // 0 - Clockwise oriented patch / 1 - Counter clockwise oriented patch.
    	context.patchFaceFlag.setEnabled(normalArgument);
    }

    private void executeCommandMMS() {
        modelMatrixUpload.startUpload(normalArgument);
        if (isLogDebugEnabled) {
            log("sceGumMatrixMode GU_MODEL " + normalArgument);
        }
    }

    private void executeCommandMODEL() {
        if (modelMatrixUpload.uploadValue(floatArgument(normalArgument))) {
            log("glLoadMatrixf", context.model_uploaded_matrix);
        }
    }

    private void executeCommandVMS() {
        viewMatrixUpload.startUpload(normalArgument);
        if (isLogDebugEnabled) {
            log("sceGumMatrixMode GU_VIEW " + normalArgument);
        }
    }

    private void executeCommandVIEW() {
        if (viewMatrixUpload.uploadValue(floatArgument(normalArgument))) {
            log("glLoadMatrixf", context.view_uploaded_matrix);
        }
    }

    private void executeCommandPMS() {
        projectionMatrixUpload.startUpload(normalArgument);
        if (isLogDebugEnabled) {
            log("sceGumMatrixMode GU_PROJECTION " + normalArgument);
        }
    }

    private void executeCommandPROJ() {
        if (projectionMatrixUpload.uploadValue(floatArgument(normalArgument))) {
            log("glLoadMatrixf", context.proj_uploaded_matrix);
        }
    }

    private void executeCommandTMS() {
        textureMatrixUpload.startUpload(normalArgument);
        if (isLogDebugEnabled) {
            log("sceGumMatrixMode GU_TEXTURE " + normalArgument);
        }
    }

    private void executeCommandTMATRIX() {
        if (textureMatrixUpload.uploadValue(floatArgument(normalArgument))) {
            log("glLoadMatrixf", context.texture_uploaded_matrix);
        }
    }

    private void executeCommandXSCALE() {
        int old_viewport_width = context.viewport_width;
        context.viewport_width = (int) floatArgument(normalArgument);
        if (old_viewport_width != context.viewport_width) {
            viewportChanged = true;
            if ((old_viewport_width < 0 && context.viewport_width > 0) ||
                (old_viewport_width > 0 && context.viewport_width < 0)) {
            	// Projection matrix has to be reloaded when X-axis flipped
            	projectionMatrixUpload.setChanged(true);
            }
        }
    }

    private void executeCommandYSCALE() {
        int old_viewport_height = context.viewport_height;
        context.viewport_height = (int) floatArgument(normalArgument);
        if (old_viewport_height != context.viewport_height) {
            viewportChanged = true;
            if ((old_viewport_height < 0 && context.viewport_height > 0) ||
                (old_viewport_height > 0 && context.viewport_height < 0)) {
            	// Projection matrix has to be reloaded when Y-axis flipped
            	projectionMatrixUpload.setChanged(true);
            }
        }

        if (isLogDebugEnabled) {
            log.debug("sceGuViewport(cx=" + context.viewport_cx + ", cy=" + context.viewport_cy + ", w=" + context.viewport_width + ", h=" + context.viewport_height + ")");
        }
    }

    private void executeCommandZSCALE() {
        float old_zscale = context.zscale;
        float floatArgument = floatArgument(normalArgument);
        context.zscale = floatArgument / 65535.f;
        if (old_zscale != context.zscale) {
            depthChanged = true;
        }

        if (isLogDebugEnabled) {
            log(helper.getCommandString(ZSCALE) + " " + floatArgument);
        }
    }

    private void executeCommandXPOS() {
        int old_viewport_cx = context.viewport_cx;
        context.viewport_cx = (int) floatArgument(normalArgument);
        if (old_viewport_cx != context.viewport_cx) {
            viewportChanged = true;
        }
    }

    private void executeCommandYPOS() {
        int old_viewport_cy = context.viewport_cy;
        context.viewport_cy = (int) floatArgument(normalArgument);
        if (old_viewport_cy != context.viewport_cy) {
            viewportChanged = true;
        }

        if (isLogDebugEnabled) {
            log.debug("sceGuViewport(cx=" + context.viewport_cx + ", cy=" + context.viewport_cy + ", w=" + context.viewport_width + ", h=" + context.viewport_height + ")");
        }
    }

    private void executeCommandZPOS() {
        float old_zpos = context.zpos;
        float floatArgument = floatArgument(normalArgument);
        context.zpos = floatArgument / 65535.f;
        if (old_zpos != context.zpos) {
            depthChanged = true;
        }

        if (isLogDebugEnabled) {
            log(helper.getCommandString(ZPOS), floatArgument);
        }
    }

    private void executeCommandUSCALE() {
        float old_tex_scale_x = context.tex_scale_x;
        context.tex_scale_x = floatArgument(normalArgument);

        if (old_tex_scale_x != context.tex_scale_x) {
            textureMatrixUpload.setChanged(true);
        }
    }

    private void executeCommandVSCALE() {
        float old_tex_scale_y = context.tex_scale_y;
        context.tex_scale_y = floatArgument(normalArgument);

        if (old_tex_scale_y != context.tex_scale_y) {
            textureMatrixUpload.setChanged(true);
        }

        if (isLogDebugEnabled) {
            log("sceGuTexScale(u=" + context.tex_scale_x + ", v=" + context.tex_scale_y + ")");
        }
    }

    private void executeCommandUOFFSET() {
        float old_tex_translate_x = context.tex_translate_x;
        context.tex_translate_x = floatArgument(normalArgument);

        if (old_tex_translate_x != context.tex_translate_x) {
            textureMatrixUpload.setChanged(true);
        }
    }

    private void executeCommandVOFFSET() {
        float old_tex_translate_y = context.tex_translate_y;
        context.tex_translate_y = floatArgument(normalArgument);

        if (old_tex_translate_y != context.tex_translate_y) {
            textureMatrixUpload.setChanged(true);
        }

        if (isLogDebugEnabled) {
            log("sceGuTexOffset(u=" + context.tex_translate_x + ", v=" + context.tex_translate_y + ")");
        }
    }

    private void executeCommandOFFSETX() {
        int old_offset_x = context.offset_x;
        context.offset_x = normalArgument >> 4;
        if (old_offset_x != context.offset_x) {
            viewportChanged = true;
        }
    }

    private void executeCommandOFFSETY() {
        int old_offset_y = context.offset_y;
        context.offset_y = normalArgument >> 4;
        if (old_offset_y != context.offset_y) {
            viewportChanged = true;
        }

        if(isLogDebugEnabled) {
            log.debug("sceGuOffset(x=" + context.offset_x + ",y=" + context.offset_y + ")");
        }
    }

    private void executeCommandSHADE() {
    	context.shadeModel = normalArgument & 1;
        re.setShadeModel(context.shadeModel);
        if (isLogDebugEnabled) {
            log("sceGuShadeModel(" + ((context.shadeModel != 0) ? "smooth" : "flat") + ")");
        }
    }

    private void executeCommandRNORM() {
        // This seems to be taked into account when calculating the lighting
        // for the current normal.
    	context.faceNormalReverseFlag.setEnabled(normalArgument);
    }

    private void executeCommandCMAT() {
        int old_mat_flags = context.mat_flags;
        context.mat_flags = normalArgument & 7;
        if (old_mat_flags != context.mat_flags) {
            materialChanged = true;
        }

        if (isLogDebugEnabled) {
            log("sceGuColorMaterial " + context.mat_flags);
        }
    }

    private void executeCommandEMC() {
    	context.mat_emissive[0] = ((normalArgument) & 255) / 255.f;
    	context.mat_emissive[1] = ((normalArgument >> 8) & 255) / 255.f;
    	context.mat_emissive[2] = ((normalArgument >> 16) & 255) / 255.f;
    	context.mat_emissive[3] = 1.f;
        materialChanged = true;
        re.setMaterialEmissiveColor(context.mat_emissive);
        if (isLogDebugEnabled) {
            log("material emission " + String.format("r=%.1f g=%.1f b=%.1f (%08X)",
            		context.mat_emissive[0], context.mat_emissive[1], context.mat_emissive[2], normalArgument));
        }
    }

    private void executeCommandAMC() {
        context.mat_ambient[0] = ((normalArgument) & 255) / 255.f;
        context.mat_ambient[1] = ((normalArgument >> 8) & 255) / 255.f;
        context.mat_ambient[2] = ((normalArgument >> 16) & 255) / 255.f;
        materialChanged = true;
        if (isLogDebugEnabled) {
            log(String.format("material ambient r=%.1f g=%.1f b=%.1f (%08X)",
                    context.mat_ambient[0], context.mat_ambient[1], context.mat_ambient[2], normalArgument));
        }
    }

    private void executeCommandDMC() {
        context.mat_diffuse[0] = ((normalArgument) & 255) / 255.f;
        context.mat_diffuse[1] = ((normalArgument >> 8) & 255) / 255.f;
        context.mat_diffuse[2] = ((normalArgument >> 16) & 255) / 255.f;
        context.mat_diffuse[3] = 1.f;
        materialChanged = true;
        if (isLogDebugEnabled) {
            log("material diffuse " + String.format("r=%.1f g=%.1f b=%.1f (%08X)",
                    context.mat_diffuse[0], context.mat_diffuse[1], context.mat_diffuse[2], normalArgument));
        }
    }

    private void executeCommandSMC() {
        context.mat_specular[0] = ((normalArgument) & 255) / 255.f;
        context.mat_specular[1] = ((normalArgument >> 8) & 255) / 255.f;
        context.mat_specular[2] = ((normalArgument >> 16) & 255) / 255.f;
        context.mat_specular[3] = 1.f;
        materialChanged = true;
        if (isLogDebugEnabled) {
            log("material specular " + String.format("r=%.1f g=%.1f b=%.1f (%08X)",
                    context.mat_specular[0], context.mat_specular[1], context.mat_specular[2], normalArgument));
        }
    }

    private void executeCommandAMA() {
        context.mat_ambient[3] = ((normalArgument) & 255) / 255.f;
        materialChanged = true;
        if (isLogDebugEnabled) {
            log(String.format("material ambient a=%.1f (%02X)",
                    context.mat_ambient[3], normalArgument & 255));
        }
    }

    private void executeCommandSPOW() {
        context.materialShininess = floatArgument(normalArgument);
        re.setMaterialShininess(context.materialShininess);
        if (isLogDebugEnabled) {
            log("material shininess " + context.materialShininess);
        }
    }

    private void executeCommandALC() {
    	context.ambient_light[0] = ((normalArgument) & 255) / 255.f;
    	context.ambient_light[1] = ((normalArgument >> 8) & 255) / 255.f;
    	context.ambient_light[2] = ((normalArgument >> 16) & 255) / 255.f;
        re.setLightModelAmbientColor(context.ambient_light);
        if (isLogDebugEnabled) {
            log.debug(String.format("ambient light r=%.1f g=%.1f b=%.1f (%06X)",
            		context.ambient_light[0], context.ambient_light[1], context.ambient_light[2], normalArgument));
        }
    }

    private void executeCommandALA() {
    	context.ambient_light[3] = ((normalArgument) & 255) / 255.f;
        re.setLightModelAmbientColor(context.ambient_light);
    }

    private void executeCommandLMODE() {
    	context.lightMode = normalArgument & 1;
        re.setLightMode(context.lightMode);
        if (isLogDebugEnabled) {
            log.debug("sceGuLightMode(" + ((context.lightMode != 0) ? "GU_SEPARATE_SPECULAR_COLOR" : "GU_SINGLE_COLOR") + ")");
        }

        // Check if other values than 0 and 1 are set
        if ((normalArgument & ~1) != 0) {
            log.warn(String.format("Unknown light mode sceGuLightMode(%06X)", normalArgument));
        }
    }

    private void executeCommandLTn() {
        int lnum = command - LT0;
        int old_light_type = context.light_type[lnum];
        int old_light_kind = context.light_kind[lnum];
        context.light_type[lnum] = (normalArgument >> 8) & 3;
        context.light_kind[lnum] = normalArgument & 3;

        if (old_light_type != context.light_type[lnum] || old_light_kind != context.light_kind[lnum]) {
            lightingChanged = true;
        }

        switch (context.light_type[lnum]) {
            case LIGHT_DIRECTIONAL:
            	context.light_pos[lnum][3] = 0.f;
                break;
            case LIGHT_POINT:
            	re.setLightSpotCutoff(lnum, 180);
            	context.light_pos[lnum][3] = 1.f;
                break;
            case LIGHT_SPOT:
            	context.light_pos[lnum][3] = 1.f;
                break;
            default:
                error("Unknown light type : " + normalArgument);
        }
        re.setLightType(lnum, context.light_type[lnum], context.light_kind[lnum]);

        if (isLogDebugEnabled) {
            log.debug("Light " + lnum + " type " + (normalArgument >> 8) + " kind " + (normalArgument & 3));
        }
    }

    private void executeCommandLXPn() {
        int lnum = (command - LXP0) / 3;
        int component = (command - LXP0) % 3;
        float old_light_pos = context.light_pos[lnum][component];
        context.light_pos[lnum][component] = floatArgument(normalArgument);

        if (old_light_pos != context.light_pos[lnum][component]) {
            lightingChanged = true;

            // Environment mapping is using light positions
            if (context.tex_map_mode == TMAP_TEXTURE_MAP_MODE_ENVIRONMENT_MAP) {
            	if (context.tex_shade_u == lnum || context.tex_shade_v == lnum) {
            		textureMatrixUpload.setChanged(true);
            	}
            }
        }
        if (isLogDebugEnabled) {
            log.debug(String.format("Light %d position (%f, %f, %f)", lnum, context.light_pos[lnum][0], context.light_pos[lnum][1], context.light_pos[lnum][2]));
        }
    }

    private void executeCommandLXDn() {
        int lnum = (command - LXD0) / 3;
        int component = (command - LXD0) % 3;
        float old_light_dir = context.light_dir[lnum][component];

        // OpenGL requires a normal in the opposite direction as the PSP
        context.light_dir[lnum][component] = -floatArgument(normalArgument);

        if (old_light_dir != context.light_dir[lnum][component]) {
            lightingChanged = true;
        }
        if (isLogDebugEnabled) {
            log.debug(String.format("Light %d direction (%f, %f, %f)", lnum, context.light_dir[lnum][0], context.light_dir[lnum][1], context.light_dir[lnum][2]));
        }
        // OpenGL parameter for light direction is set in initRendering
        // because it depends on the model/view matrix
    }

    private void executeCommandLCAn() {
        int lnum = (command - LCA0) / 3;
        context.lightConstantAttenuation[lnum] = floatArgument(normalArgument);
        re.setLightConstantAttenuation(lnum, context.lightConstantAttenuation[lnum]);
    }

    private void executeCommandLLAn() {
        int lnum = (command - LLA0) / 3;
        context.lightLinearAttenuation[lnum] = floatArgument(normalArgument);
        re.setLightLinearAttenuation(lnum, context.lightLinearAttenuation[lnum]);
    }

    private void executeCommandLQAn() {
        int lnum = (command - LQA0) / 3;
        context.lightQuadraticAttenuation[lnum] = floatArgument(normalArgument);
        re.setLightQuadraticAttenuation(lnum, context.lightQuadraticAttenuation[lnum]);
    }

    private void executeCommandSLEn() {
        int lnum = command - SLE0;
        float old_spotLightExponent = context.spotLightExponent[lnum];
        context.spotLightExponent[lnum] = floatArgument(normalArgument);

        if (old_spotLightExponent != context.spotLightExponent[lnum]) {
            lightingChanged = true;
        }

        if (isLogDebugEnabled) {
            VideoEngine.log.debug("sceGuLightSpot(" + lnum + ",X," + context.spotLightExponent[lnum] + ",X)");
        }
    }

    private void executeCommandSLFn() {
        int lnum = command - SLF0;
        float old_spotLightCutoff = context.spotLightCutoff[lnum];

        // PSP Cutoff is cosine of angle, OpenGL expects degrees
        float floatArgument = floatArgument(normalArgument);
        float degreeCutoff = (float) Math.toDegrees(Math.acos(floatArgument));
        if ((degreeCutoff >= 0 && degreeCutoff <= 90) || degreeCutoff == 180) {
        	context.spotLightCutoff[lnum] = degreeCutoff;

            if (old_spotLightCutoff != context.spotLightCutoff[lnum]) {
                lightingChanged = true;
            }

            if (isLogDebugEnabled) {
                log.debug("sceGuLightSpot(" + lnum + ",X,X," + floatArgument + "=" + degreeCutoff + ")");
            }
        } else {
            log.warn("sceGuLightSpot(" + lnum + ",X,X," + floatArgument + ") invalid argument value");
        }
    }

    private void executeCommandALCn() {
        int lnum = (command - ALC0) / 3;
        context.lightAmbientColor[lnum][0] = ((normalArgument) & 255) / 255.f;
        context.lightAmbientColor[lnum][1] = ((normalArgument >> 8) & 255) / 255.f;
        context.lightAmbientColor[lnum][2] = ((normalArgument >> 16) & 255) / 255.f;
        context.lightAmbientColor[lnum][3] = 1.f;
        re.setLightAmbientColor(lnum, context.lightAmbientColor[lnum]);
        log("sceGuLightColor (GU_LIGHT0, GU_AMBIENT)");
    }

    private void executeCommandDLCn() {
        int lnum = (command - DLC0) / 3;
        context.lightDiffuseColor[lnum][0] = ((normalArgument) & 255) / 255.f;
        context.lightDiffuseColor[lnum][1] = ((normalArgument >> 8) & 255) / 255.f;
        context.lightDiffuseColor[lnum][2] = ((normalArgument >> 16) & 255) / 255.f;
        context.lightDiffuseColor[lnum][3] = 1.f;
        re.setLightDiffuseColor(lnum, context.lightDiffuseColor[lnum]);
        log("sceGuLightColor (GU_LIGHT0, GU_DIFFUSE)");
    }

    private void executeCommandSLCn() {
        int lnum = (command - SLC0) / 3;
        float old_lightSpecularColor0 = context.lightSpecularColor[lnum][0];
        float old_lightSpecularColor1 = context.lightSpecularColor[lnum][1];
        float old_lightSpecularColor2 = context.lightSpecularColor[lnum][2];
        context.lightSpecularColor[lnum][0] = ((normalArgument) & 255) / 255.f;
        context.lightSpecularColor[lnum][1] = ((normalArgument >> 8) & 255) / 255.f;
        context.lightSpecularColor[lnum][2] = ((normalArgument >> 16) & 255) / 255.f;
        context.lightSpecularColor[lnum][3] = 1.f;

        if (old_lightSpecularColor0 != context.lightSpecularColor[lnum][0] || old_lightSpecularColor1 != context.lightSpecularColor[lnum][1] || old_lightSpecularColor2 != context.lightSpecularColor[lnum][2]) {
            lightingChanged = true;
        }
        re.setLightSpecularColor(lnum, context.lightDiffuseColor[lnum]);
        log("sceGuLightColor (GU_LIGHT0, GU_SPECULAR)");
    }

    private void executeCommandFFACE() {
    	context.frontFaceCw = normalArgument != 0;
        re.setFrontFace(context.frontFaceCw);
        if (isLogDebugEnabled) {
            log(helper.getCommandString(FFACE) + " " + ((normalArgument != 0) ? "clockwise" : "counter-clockwise"));
        }
    }

    private void executeCommandFBP() {
        // FBP can be called before or after FBW
    	context.fbp = (context.fbp & 0xff000000) | normalArgument;
        if (isLogDebugEnabled) {
            log(helper.getCommandString(FBP) + " fbp=" + Integer.toHexString(context.fbp) + ", fbw=" + context.fbw);
        }
        geBufChanged = true;
    }

    private void executeCommandFBW() {
    	context.fbp = (context.fbp & 0x00ffffff) | ((normalArgument << 8) & 0xff000000);
    	context.fbw = normalArgument & 0xffff;
        if (isLogDebugEnabled) {
            log(helper.getCommandString(FBW) + " fbp=" + Integer.toHexString(context.fbp) + ", fbw=" + context.fbw);
        }
        geBufChanged = true;
    }

    private void executeCommandZBP() {
    	context.zbp = (context.zbp & 0xff000000) | normalArgument;
        if (isLogDebugEnabled) {
            log("zbp=" + Integer.toHexString(context.zbp) + ", zbw=" +context. zbw);
        }
    }

    private void executeCommandZBW() {
    	context.zbp = (context.zbp & 0x00ffffff) | ((normalArgument << 8) & 0xff000000);
        context.zbw = normalArgument & 0xffff;
        if (isLogDebugEnabled) {
            log("zbp=" + Integer.toHexString(context.zbp) + ", zbw=" + context.zbw);
        }
    }

    private void executeCommandTBPn() {
        int level = command - TBP0;
        int old_texture_base_pointer = context.texture_base_pointer[level];
        context.texture_base_pointer[level] = (context.texture_base_pointer[level] & 0xff000000) | normalArgument;

        if (old_texture_base_pointer != context.texture_base_pointer[level]) {
            textureChanged = true;
        }

        if (isLogDebugEnabled) {
            log(String.format("sceGuTexImage(level=%d, X, X, X, lo(pointer=0x%08X)", level, context.texture_base_pointer[level]));
        }
    }

    private void executeCommandTBWn() {
        int level = command - TBW0;
        int old_texture_base_pointer = context.texture_base_pointer[level];
        int old_texture_buffer_width = context.texture_buffer_width[level];
        context.texture_base_pointer[level] = (context.texture_base_pointer[level] & 0x00ffffff) | ((normalArgument << 8) & 0xff000000);
        context.texture_buffer_width[level] = normalArgument & 0xffff;

        if (old_texture_base_pointer != context.texture_base_pointer[level] || old_texture_buffer_width != context.texture_buffer_width[level]) {
            textureChanged = true;
        }

        if (isLogDebugEnabled) {
            log(String.format("sceGuTexImage(level=%d, X, X, texBufferWidth=%d, hi(pointer=0x%08X))", level, context.texture_buffer_width[level], context.texture_base_pointer[level]));
        }
    }

    private void executeCommandCBP() {
        int old_tex_clut_addr = context.tex_clut_addr;
        context.tex_clut_addr = (context.tex_clut_addr & 0xff000000) | normalArgument;

        clutIsDirty = true;
        if (old_tex_clut_addr != context.tex_clut_addr) {
            textureChanged = true;
        }

        if (isLogDebugEnabled) {
            log("sceGuClutLoad(X, lo(cbp=0x" + Integer.toHexString(context.tex_clut_addr) + "))");
        }
    }

    private void executeCommandCBPH() {
        int old_tex_clut_addr = context.tex_clut_addr;
        context.tex_clut_addr = (context.tex_clut_addr & 0x00ffffff) | ((normalArgument << 8) & 0x0f000000);

        clutIsDirty = true;
        if (old_tex_clut_addr != context.tex_clut_addr) {
            textureChanged = true;
        }

        if (isLogDebugEnabled) {
            log("sceGuClutLoad(X, hi(cbp=0x" + Integer.toHexString(context.tex_clut_addr) + "))");
        }
    }

    private void executeCommandTRXSBP() {
        context.textureTx_sourceAddress = (context.textureTx_sourceAddress & 0xFF000000) | normalArgument;
        if (isLogDebugEnabled) {
        	log.debug(String.format("%s sourceAddress=0x%08X", helper.getCommandString(command), context.textureTx_sourceAddress));
        }
    }

    private void executeCommandTRXSBW() {
        context.textureTx_sourceAddress = (context.textureTx_sourceAddress & 0x00FFFFFF) | ((normalArgument << 8) & 0xFF000000);
        context.textureTx_sourceLineWidth = normalArgument & 0x0000FFFF;
        if (isLogDebugEnabled) {
        	log.debug(String.format("%s sourceAddress=0x%08X, sourceLineWidth=%d", helper.getCommandString(command), context.textureTx_sourceAddress, context.textureTx_sourceLineWidth));
        }

        // TODO Check when sx and sy are reset to 0. Here or after TRXKICK?
        context.textureTx_sx = 0;
        context.textureTx_sy = 0;
    }

    private void executeCommandTRXDBP() {
        context.textureTx_destinationAddress = (context.textureTx_destinationAddress & 0xFF000000) | normalArgument;
        if (isLogDebugEnabled) {
        	log.debug(String.format("%s destinationAddress=0x%08X", helper.getCommandString(command), context.textureTx_destinationAddress));
        }
    }

    private void executeCommandTRXDBW() {
        context.textureTx_destinationAddress = (context.textureTx_destinationAddress & 0x00FFFFFF) | ((normalArgument << 8) & 0xFF000000);
        context.textureTx_destinationLineWidth = normalArgument & 0x0000FFFF;
        if (isLogDebugEnabled) {
        	log.debug(String.format("%s destinationAddress=0x%08X, destinationLineWidth=%d", helper.getCommandString(command), context.textureTx_destinationAddress, context.textureTx_destinationLineWidth));
        }

        // TODO Check when dx and dy are reset to 0. Here or after TRXKICK?
        context.textureTx_dx = 0;
        context.textureTx_dy = 0;
    }

    private void executeCommandTSIZEn() {
        int level = command - TSIZE0;
        int old_texture_height = context.texture_height[level];
        int old_texture_width = context.texture_width[level];
        // Astonishia Story is using normalArgument = 0x1804
        // -> use texture_height = 1 << 0x08 (and not 1 << 0x18)
        //        texture_width  = 1 << 0x04
        // The maximum texture size is 512x512: the exponent value must be [0..9]
        int height_exp2 = Math.min((normalArgument >> 8) & 0x0F, 9);
        int width_exp2 = Math.min((normalArgument) & 0x0F, 9);
        context.texture_height[level] = 1 << height_exp2;
        context.texture_width[level] = 1 << width_exp2;

        if (old_texture_height != context.texture_height[level] || old_texture_width != context.texture_width[level]) {
            if (context.transform_mode == VTYPE_TRANSFORM_PIPELINE_RAW_COORD && level == 0) {
                textureMatrixUpload.setChanged(true);
            }
            textureChanged = true;
        }

        if (isLogDebugEnabled) {
            log("sceGuTexImage(level=" + level + ", width=" + context.texture_width[level] + ", height=" + context.texture_height[level] + ", X, X)");
        }
    }

    private void executeCommandTMAP() {
        int old_tex_map_mode = context.tex_map_mode;
        context.tex_map_mode = normalArgument & 3;
        context.tex_proj_map_mode = (normalArgument >> 8) & 3;

        if (old_tex_map_mode != context.tex_map_mode) {
            textureMatrixUpload.setChanged(true);
        }

        if (isLogDebugEnabled) {
            log("sceGuTexMapMode(mode=" + context.tex_map_mode + ", X, X)");
            log("sceGuTexProjMapMode(mode=" + context.tex_proj_map_mode + ")");
        }
    }

    private void executeCommandTEXTURE_ENV_MAP_MATRIX() {
    	context.tex_shade_u = (normalArgument >> 0) & 0x3;
    	context.tex_shade_v = (normalArgument >> 8) & 0x3;

        textureMatrixUpload.setChanged(true);
        if (isLogDebugEnabled) {
            log("sceGuTexMapMode(X, " + context.tex_shade_u + ", " + context.tex_shade_v + ")");
        }
    }

    private void executeCommandTMODE() {
        int old_texture_num_mip_maps = context.texture_num_mip_maps;
        boolean old_mipmapShareClut = context.mipmapShareClut;
        boolean old_texture_swizzle = context.texture_swizzle;
        context.texture_num_mip_maps = (normalArgument >> 16) & 0x7;
        // This parameter has only a meaning when
        //  texture_storage == GU_PSM_T4 and texture_num_mip_maps > 0
        // when parameter==0: all the mipmaps share the same clut entries (normal behavior)
        // when parameter==1: each mipmap has its own clut table, 16 entries each, stored sequentially
        context.mipmapShareClut = ((normalArgument >> 8) & 0x1) == 0;
        context.texture_swizzle = ((normalArgument) & 0x1) != 0;

        if (old_texture_num_mip_maps != context.texture_num_mip_maps || old_mipmapShareClut != context.mipmapShareClut || old_texture_swizzle != context.texture_swizzle) {
            textureChanged = true;
        }

        if (isLogDebugEnabled) {
            log("sceGuTexMode(X, mipmaps=" + context.texture_num_mip_maps + ", mipmapShareClut=" + context.mipmapShareClut + ", swizzle=" + context.texture_swizzle + ")");
        }
    }

    private void executeCommandTPSM() {
        int old_texture_storage = context.texture_storage;
        context.texture_storage = normalArgument & 0xF; // Lower four bits.

        if (old_texture_storage != context.texture_storage) {
            textureChanged = true;
        }

        if (isLogDebugEnabled) {
            log("sceGuTexMode(tpsm=" + context.texture_storage + "(" + getPsmName(context.texture_storage) + "), X, X, X)");
        }
    }

    private void executeCommandCLOAD() {
        int old_tex_clut_num_blocks = context.tex_clut_num_blocks;
        context.tex_clut_num_blocks = normalArgument & 0x3F;

        clutIsDirty = true;
        if (old_tex_clut_num_blocks != context.tex_clut_num_blocks) {
            textureChanged = true;
        }

        // Some games use the following sequence:
        // - sceGuClutLoad(num_blocks=32, X)
        // - sceGuClutLoad(num_blocks=1, X)
        // - tflush
        // - prim ... (texture data is referencing the clut entries from 32 blocks)
        //
        readClut();

        if (isLogDebugEnabled) {
            log("sceGuClutLoad(num_blocks=" + context.tex_clut_num_blocks + ", X)");
        }
    }

    private void executeCommandCMODE() {
        int old_tex_clut_mode = context.tex_clut_mode;
        int old_tex_clut_shift = context.tex_clut_shift;
        int old_tex_clut_mask = context.tex_clut_mask;
        int old_tex_clut_start = context.tex_clut_start;
        context.tex_clut_mode = normalArgument & 0x03;
        context.tex_clut_shift = (normalArgument >> 2) & 0x1F;
        context.tex_clut_mask = (normalArgument >> 8) & 0xFF;
        context.tex_clut_start = (normalArgument >> 16) & 0x1F;

        clutIsDirty = true;
        if (old_tex_clut_mode != context.tex_clut_mode || old_tex_clut_shift != context.tex_clut_shift || old_tex_clut_mask != context.tex_clut_mask || old_tex_clut_start != context.tex_clut_start) {
            textureChanged = true;
        }

        if (isLogDebugEnabled) {
            log("sceGuClutMode(cpsm=" + context.tex_clut_mode + "(" + getPsmName(context.tex_clut_mode) + "), shift=" + context.tex_clut_shift + ", mask=0x" + Integer.toHexString(context.tex_clut_mask) + ", start=" + context.tex_clut_start + ")");
        }
    }

    private void executeCommandTFLT() {
        int old_tex_mag_filter = context.tex_mag_filter;
        int old_tex_min_filter = context.tex_min_filter;

        context.tex_min_filter = normalArgument & 0x7;
        context.tex_mag_filter = (normalArgument >> 8) & 0x1;

        if (isLogDebugEnabled) {
            log("sceGuTexFilter(min=" + context.tex_min_filter + ", mag=" + context.tex_mag_filter + ") (mm#" + context.texture_num_mip_maps + ")");
        }

        if (context.tex_min_filter == TFLT_UNKNOW1 || context.tex_min_filter == TFLT_UNKNOW2) {
            log.warn("Unknown minimizing filter " + (normalArgument & 0xFF));
            context.tex_min_filter = TFLT_NEAREST;
        }

        if (old_tex_mag_filter != context.tex_mag_filter || old_tex_min_filter != context.tex_min_filter) {
            textureChanged = true;
        }
    }

    private void executeCommandTWRAP() {
    	context.tex_wrap_s = normalArgument & 0xFF;
    	context.tex_wrap_t = (normalArgument >> 8) & 0xFF;

        if (context.tex_wrap_s > TWRAP_WRAP_MODE_CLAMP) {
            log.warn(helper.getCommandString(TWRAP) + " unknown wrap mode " + context.tex_wrap_s);
            context.tex_wrap_s = TWRAP_WRAP_MODE_REPEAT;
        }
        if (context.tex_wrap_t > TWRAP_WRAP_MODE_CLAMP) {
            log.warn(helper.getCommandString(TWRAP) + " unknown wrap mode " + context.tex_wrap_t);
            context.tex_wrap_t = TWRAP_WRAP_MODE_REPEAT;
        }
    }

    private void executeCommandTBIAS() {
    	context.tex_mipmap_mode = normalArgument & 0x3;
    	int biasValue = (int) (byte) (normalArgument >> 16); // Signed 8-bit 4.4 fixed point value
    	context.tex_mipmap_bias_int = biasValue >> 4;
    	context.tex_mipmap_bias = biasValue / 16.0f;
        if (isLogDebugEnabled) {
            log.debug("sceGuTexLevelMode(mode=" + context.tex_mipmap_mode + ", bias=" + context.tex_mipmap_bias + ")");
        }
    }

    private void executeCommandTEC() {
    	context.tex_env_color[0] = ((normalArgument) & 255) / 255.f;
    	context.tex_env_color[1] = ((normalArgument >> 8) & 255) / 255.f;
    	context.tex_env_color[2] = ((normalArgument >> 16) & 255) / 255.f;
    	context.tex_env_color[3] = 1.f;
        re.setTextureEnvColor(context.tex_env_color);

        if (isLogDebugEnabled) {
            log(String.format("sceGuTexEnvColor %08X (no alpha)", normalArgument));
        }
    }

    private void executeCommandTFLUSH() {
        // Do not load the texture right now, clut parameters can still be
        // defined after the TFLUSH and before the PRIM command.
        // Delay the texture loading until the PRIM command.
        if (isLogDebugEnabled) {
            log("tflush (deferring to prim)");
        }
    }

    private void executeCommandTSYNC() {
        // Probably synchronizing the GE when a drawing result
    	// is used as a texture. Currently ignored.
        if (isLogDebugEnabled) {
            log(helper.getCommandString(TSYNC) + " waiting for drawing.");
        }
    }

    private void executeCommandFFAR() {
    	context.fog_far = floatArgument(normalArgument);
    }

    private void executeCommandFDIST() {
    	context.fog_dist = floatArgument(normalArgument);
        if ((context.fog_far != 0.0f) && (context.fog_dist != 0.0f)) {
            float end = context.fog_far;
            float start = end - (1 / context.fog_dist);
            re.setFogDist(start, end);
        }
    }

    private void executeCommandFCOL() {
    	context.fog_color[0] = ((normalArgument) & 255) / 255.f;
        context.fog_color[1] = ((normalArgument >> 8) & 255) / 255.f;
        context.fog_color[2] = ((normalArgument >> 16) & 255) / 255.f;
        context.fog_color[3] = 1.f;
        re.setFogColor(context.fog_color);

        if (isLogDebugEnabled) {
            log(String.format("sceGuFog(X, X, color=%08X) (no alpha)", normalArgument));
        }
    }

    private void executeCommandTSLOPE() {
    	context.tslope_level = floatArgument(normalArgument);
        if (isLogDebugEnabled) {
            log(helper.getCommandString(TSLOPE) + " tslope_level=" + context.tslope_level);
        }
    }

    private void executeCommandPSM() {
    	context.psm = normalArgument;
        if (isLogDebugEnabled) {
            log("psm=" + normalArgument + "(" + getPsmName(normalArgument) + ")");
        }
        geBufChanged = true;
    }

    private void executeCommandSCISSOR1() {
    	context.scissor_x1 = normalArgument & 0x3ff;
    	context.scissor_y1 = (normalArgument >> 10) & 0x3ff;

    	// Already update width&height in case SCISSOR2 is not coming...
    	context.scissor_width = 1 + context.scissor_x2 - context.scissor_x1;
    	context.scissor_height = 1 + context.scissor_y2 - context.scissor_y1;

    	scissorChanged = true;
    }

    private void executeCommandSCISSOR2() {
    	context.scissor_x2 = normalArgument & 0x3ff;
    	context.scissor_y2 = (normalArgument >> 10) & 0x3ff;
    	context.scissor_width = 1 + context.scissor_x2 - context.scissor_x1;
    	context.scissor_height = 1 + context.scissor_y2 - context.scissor_y1;
        if (isLogDebugEnabled) {
            log("sceGuScissor(" + context.scissor_x1 + "," + context.scissor_y1 + "," + context.scissor_width + "," + context.scissor_height + ")");
        }
    	scissorChanged = true;
    }

    private void executeCommandNEARZ() {
        float old_nearZ = context.nearZ;
        context.nearZ = (normalArgument & 0xFFFF) / (float) 0xFFFF;
        if (old_nearZ != context.nearZ) {
            depthChanged = true;
        }
    }

    private void executeCommandFARZ() {
        float old_farZ = context.farZ;
        context.farZ = (normalArgument & 0xFFFF) / (float) 0xFFFF;
        if (old_farZ != context.farZ) {
            // OpenGL requires the Depth parameters to be reloaded
            depthChanged = true;
        }

        if (depthChanged) {
            re.setDepthRange(context.zpos, context.zscale, context.nearZ, context.farZ);
        }

        if (isLogDebugEnabled) {
            log.debug("sceGuDepthRange(" + context.nearZ + ", " + context.farZ + ")");
        }
    }

    private void executeCommandCTST() {
    	context.colorTestFunc = normalArgument & 3;
        re.setColorTestFunc(context.colorTestFunc);
    }

    private void executeCommandCREF() {
    	context.colorTestRef[0] = (normalArgument) & 0xFF;
    	context.colorTestRef[1] = (normalArgument >> 8) & 0xFF;
    	context.colorTestRef[2] = (normalArgument >> 16) & 0xFF;
        re.setColorTestReference(context.colorTestRef);
    }

    private void executeCommandCMSK() {
    	context.colorTestMsk[0] = (normalArgument) & 0xFF;
        context.colorTestMsk[1] = (normalArgument >> 8) & 0xFF;
        context.colorTestMsk[2] = (normalArgument >> 16) & 0xFF;
        re.setColorTestMask(context.colorTestMsk);
    }

    private void executeCommandATST() {
        context.alphaFunc = normalArgument & 0xFF;
        if (context.alphaFunc > ATST_PASS_PIXEL_IF_GREATER_OR_EQUAL) {
            log.warn("sceGuAlphaFunc unhandled func " + context.alphaFunc);
            context.alphaFunc = ATST_ALWAYS_PASS_PIXEL;
        }
        context.alphaRef = (normalArgument >> 8) & 0xFF;
        re.setAlphaFunc(context.alphaFunc, context.alphaRef);

        if (isLogDebugEnabled) {
        	log("sceGuAlphaFunc(" + context.alphaFunc + "," + context.alphaRef + ")");
        }
    }

    private void executeCommandSTST() {
    	context.stencilFunc = normalArgument & 0xFF;
    	if (context.stencilFunc > STST_FUNCTION_PASS_TEST_IF_GREATER_OR_EQUAL) {
    		log.warn("Unknown stencil function " + context.stencilFunc);
    		context.stencilFunc = STST_FUNCTION_ALWAYS_PASS_STENCIL_TEST;
    	}

        if (context.psm == 0) {  // PSM_5650
            context.stencilRef = 0;
        } else if (context.psm == 1) {
            context.stencilRef = (normalArgument >> 8) & 0x10; // PSM_5551
        } else if (context.psm == 2) {
            context.stencilRef = (normalArgument >> 8) & 0xF0; // PSM_4444
        } else if (context.psm == 3) {
            context.stencilRef = (normalArgument >> 8) & 0xFF; // PSM_8888
        }
        context.stencilMask = (normalArgument >> 16) & 0xFF;
        re.setStencilFunc(context.stencilFunc, context.stencilRef, context.stencilMask);

        if (isLogDebugEnabled) {
        	log("sceGuStencilFunc(func=" + (normalArgument & 0xFF) + ", ref=" + context.stencilRef + ", mask=" + context.stencilMask + ")");
        }
    }

    private void executeCommandSOP() {
        context.stencilOpFail = normalArgument & 0xFF;
        context.stencilOpZFail = (normalArgument >> 8) & 0xFF;
        context.stencilOpZPass = (normalArgument >> 16) & 0xFF;

        if (context.stencilOpFail > SOP_DECREMENT_STENCIL_VALUE) {
        	log.warn("Unknown stencil operation " + context.stencilOpFail);
        	context.stencilOpFail = SOP_KEEP_STENCIL_VALUE;
        }
        if (context.stencilOpZFail > SOP_DECREMENT_STENCIL_VALUE) {
        	log.warn("Unknown stencil operation " + context.stencilOpZFail);
        	context.stencilOpZFail = SOP_KEEP_STENCIL_VALUE;
        }
        if (context.stencilOpZPass > SOP_DECREMENT_STENCIL_VALUE) {
        	log.warn("Unknown stencil operation " + context.stencilOpZPass);
        	context.stencilOpZPass = SOP_KEEP_STENCIL_VALUE;
        }

        re.setStencilOp(context.stencilOpFail, context.stencilOpZFail, context.stencilOpZPass);

        if (isLogDebugEnabled) {
        	log("sceGuStencilOp(fail=" + (normalArgument & 0xFF) + ", zfail=" + ((normalArgument >> 8) & 0xFF) + ", zpass=" + ((normalArgument >> 16) & 0xFF) + ")");
        }
    }

    private void executeCommandZTST() {
        int oldDepthFunc = context.depthFunc;

        context.depthFunc = normalArgument & 0xFF;
        if (context.depthFunc > ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_GREATER_OR_EQUAL) {
        	error(String.format("%s unknown depth function %d", commandToString(ZTST), context.depthFunc));
        	context.depthFunc = ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_LESS;
        }

        if (oldDepthFunc != context.depthFunc) {
            depthChanged = true;
        }

        if (isLogDebugEnabled) {
            log("sceGuDepthFunc(" + normalArgument + ")");
        }
    }

    private void executeCommandALPHA() {
        context.blend_src = normalArgument & 0xF;
        context.blend_dst = (normalArgument >> 4) & 0xF;
        context.blendEquation = (normalArgument >> 8) & 0xF;

        if (context.blendEquation > ALPHA_SOURCE_BLEND_OPERATION_ABSOLUTE_VALUE) {
            log.warn("Unhandled blend operation " + context.blendEquation);
            context.blendEquation = ALPHA_SOURCE_BLEND_OPERATION_ADD;
        }
    	if (context.blend_src > ALPHA_FIX) {
            error("Unhandled alpha blend src used " + context.blend_src);
            context.blend_src = ALPHA_SOURCE_ALPHA;
    	}
    	if (context.blend_dst > ALPHA_FIX) {
            error("Unhandled alpha blend dst used " + context.blend_dst);
            context.blend_dst = ALPHA_ONE_MINUS_SOURCE_ALPHA;
    	}

        re.setBlendEquation(context.blendEquation);
        re.setBlendFunc(context.blend_src, context.blend_dst);

        if (isLogDebugEnabled) {
            log("sceGuBlendFunc(op=" + context.blendEquation + ", src=" + context.blend_src + ", dst=" + context.blend_dst + ")");
        }
    }

    private void executeCommandSFIX() {
        context.sfix_color[0] = ((normalArgument) & 255) / 255.f;
        context.sfix_color[1] = ((normalArgument >> 8) & 255) / 255.f;
        context.sfix_color[2] = ((normalArgument >> 16) & 255) / 255.f;
        context.sfix_color[3] = 1.f;

        re.setBlendSFix(context.sfix_color);

        if (isLogDebugEnabled) {
            log(String.format("%s : 0x%06X", helper.getCommandString(SFIX), normalArgument));
        }
    }

    private void executeCommandDFIX() {
        context.dfix_color[0] = ((normalArgument) & 255) / 255.f;
        context.dfix_color[1] = ((normalArgument >> 8) & 255) / 255.f;
        context.dfix_color[2] = ((normalArgument >> 16) & 255) / 255.f;
        context.dfix_color[3] = 1.f;

        re.setBlendDFix(context.dfix_color);

        if (isLogDebugEnabled) {
            log(String.format("%s : 0x%06X", helper.getCommandString(DFIX), normalArgument));
        }
    }

    private void executeCommandDTH0() {
    	context.dither_matrix[0] = (normalArgument) & 0xF;
        context.dither_matrix[1] = (normalArgument >> 4) & 0xF;
        context.dither_matrix[2] = (normalArgument >> 8) & 0xF;
        context.dither_matrix[3] = (normalArgument >> 12) & 0xF;
    }

    private void executeCommandDTH1() {
    	context.dither_matrix[4] = (normalArgument) & 0xF;
        context.dither_matrix[5] = (normalArgument >> 4) & 0xF;
        context.dither_matrix[6] = (normalArgument >> 8) & 0xF;
        context.dither_matrix[7] = (normalArgument >> 12) & 0xF;
    }

    private void executeCommandDTH2() {
    	context.dither_matrix[8] = (normalArgument) & 0xF;
        context.dither_matrix[9] = (normalArgument >> 4) & 0xF;
        context.dither_matrix[10] = (normalArgument >> 8) & 0xF;
        context.dither_matrix[11] = (normalArgument >> 12) & 0xF;
    }

    private void executeCommandDTH3() {
    	context.dither_matrix[12] = (normalArgument) & 0xF;
        context.dither_matrix[13] = (normalArgument >> 4) & 0xF;
        context.dither_matrix[14] = (normalArgument >> 8) & 0xF;
        context.dither_matrix[15] = (normalArgument >> 12) & 0xF;

        // The dither matrix's values can vary between -8 and 7.
        // The most significant bit acts as sign bit.
        // Translate and log only at the last command.

        for (int i = 0; i < 16; i++) {
            if (context.dither_matrix[i] > 7) {
            	context.dither_matrix[i] |= 0xFFFFFFF0;
            }
        }

        if (isLogDebugEnabled) {
            log.debug("DTH0:" + "  " + context.dither_matrix[0] + "  " + context.dither_matrix[1] + "  " + context.dither_matrix[2] + "  " + context.dither_matrix[3]);
            log.debug("DTH1:" + "  " + context.dither_matrix[4] + "  " + context.dither_matrix[5] + "  " + context.dither_matrix[6] + "  " + context.dither_matrix[7]);
            log.debug("DTH2:" + "  " + context.dither_matrix[8] + "  " + context.dither_matrix[9] + "  " + context.dither_matrix[10] + "  " + context.dither_matrix[11]);
            log.debug("DTH3:" + "  " + context.dither_matrix[12] + "  " + context.dither_matrix[13] + "  " + context.dither_matrix[14] + "  " + context.dither_matrix[15]);
        }
    }

    private void executeCommandLOP() {
    	context.logicOp = normalArgument & 0xF;
    	re.setLogicOp(context.logicOp);
    	if (isLogDebugEnabled) {
    		log.debug("sceGuLogicalOp(LogicOp=" + context.logicOp + "(" + getLOpName(context.logicOp) + "))");
    	}
    }

    private void executeCommandZMSK() {
        // NOTE: PSP depth mask as 1 is meant to avoid depth writes,
        //       with OpenGL it's the opposite
    	context.depthMask = (normalArgument == 0);
    	re.setDepthMask(context.depthMask);
    	if (context.depthMask) {
            // OpenGL requires the Depth parameters to be reloaded
            depthChanged = true;
    	}

        if (isLogDebugEnabled) {
            log("sceGuDepthMask(" + (normalArgument != 0 ? "disableWrites" : "enableWrites") + ")");
        }
    }

    private void executeCommandPMSKC() {
        context.colorMask[0] = normalArgument & 0xFF;
        context.colorMask[1] = (normalArgument >> 8) & 0xFF;
        context.colorMask[2] = (normalArgument >> 16) & 0xFF;
    	re.setColorMask(context.colorMask[0], context.colorMask[1], context.colorMask[2], context.colorMask[3]);

    	if (isLogDebugEnabled) {
            log(String.format("%s color mask=0x%06X", helper.getCommandString(PMSKC), normalArgument));
        }
    }

    private void executeCommandPMSKA() {
        context.colorMask[3] = normalArgument & 0xFF;
    	re.setColorMask(context.colorMask[0], context.colorMask[1], context.colorMask[2], context.colorMask[3]);

        if (isLogDebugEnabled) {
            log(String.format("%s alpha mask=0x%02X", helper.getCommandString(PMSKA), normalArgument));
        }
    }

    private void executeCommandTRXPOS() {
        context.textureTx_sx = normalArgument & 0x1FF;
        context.textureTx_sy = (normalArgument >> 10) & 0x1FF;
    }

    private void executeCommandTRXDPOS() {
        context.textureTx_dx = normalArgument & 0x1FF;
        context.textureTx_dy = (normalArgument >> 10) & 0x1FF;
        if (isLogDebugEnabled) {
        	log.debug(String.format("%s dx=%d, dy=%d", helper.getCommandString(command), context.textureTx_dx, context.textureTx_dy));
        }
    }

    private void executeCommandTRXSIZE() {
        context.textureTx_width = (normalArgument & 0x3FF) + 1;
        context.textureTx_height = ((normalArgument >> 10) & 0x3FF) + 1;
        if (isLogDebugEnabled) {
        	log.debug(String.format("%s width=%d, height=%d", helper.getCommandString(command), context.textureTx_width, context.textureTx_height));
        }
    }

    private void executeCommandVSCX() {
        int coordX = normalArgument & 0xFFFF;
        log.warn("Unimplemented VSCX: coordX=" + coordX);
    }

    private void executeCommandVSCY() {
        int coordY = normalArgument & 0xFFFF;
        log.warn("Unimplemented VSCY: coordY=" + coordY);
    }

    private void executeCommandVSCZ() {
        int coordZ = normalArgument & 0xFFFF;
        log.warn("Unimplemented VSCZ: coordZ=" + coordZ);
    }

    private void executeCommandVTCS() {
        float coordS = floatArgument(normalArgument);
        log.warn("Unimplemented VTCS: coordS=" + coordS);
    }

    private void executeCommandVTCT() {
        float coordT = floatArgument(normalArgument);
        log.warn("Unimplemented VTCT: coordT=" + coordT);
    }

    private void executeCommandVTCQ() {
        float coordQ = floatArgument(normalArgument);
        log.warn("Unimplemented VTCQ: coordQ=" + coordQ);
    }

    private void executeCommandVCV() {
        int colorR = normalArgument & 0xFF;
        int colorG = (normalArgument >> 8) & 0xFF;
        int colorB = (normalArgument >> 16) & 0xFF;
        log.warn("Unimplemented VCV: colorR=" + colorR + ", colorG=" + colorG + ", colorB=" + colorB);
    }

    private void executeCommandVAP() {
        int alpha = normalArgument & 0xFF;
        int prim_type = (normalArgument >> 8) & 0x7;
        log.warn("Unimplemented VAP: alpha=" + alpha + ", prim_type=" + prim_type);
    }

    private void executeCommandVFC() {
        int fog = normalArgument & 0xFF;
        log.warn("Unimplemented VFC: fog=" + fog);
    }

    private void executeCommandVSCV() {
        int colorR2 = normalArgument & 0xFF;
        int colorG2 = (normalArgument >> 8) & 0xFF;
        int colorB2 = (normalArgument >> 16) & 0xFF;
        log.warn("Unimplemented VSCV: colorR2=" + colorR2 + ", colorG2=" + colorG2 + ", colorB2=" + colorB2);
    }

    private void executeCommandDUMMY() {
        // This command always appears before a BOFS command and seems to have
        // no special meaning.
        // The command also appears sometimes after a PRIM command.

        // Confirmed on PSP to be a dummy command and can be safely ignored.
        // This commands' normalArgument may not be always 0, as it's totally
        // discarded on the PSP.
        if (isLogDebugEnabled) {
            log.debug("Ignored DUMMY video command.");
        }
    }

    private void enableClientState(boolean useVertexColor, boolean useTexture) {
        if (vinfo.texture != 0 || useTexture) {
            re.enableClientState(IRenderingEngine.RE_TEXTURE);
        } else {
        	re.disableClientState(IRenderingEngine.RE_TEXTURE);
        }
        if (useVertexColor) {
            re.enableClientState(IRenderingEngine.RE_COLOR);
        } else {
            re.disableClientState(IRenderingEngine.RE_COLOR);
        }
        if (vinfo.normal != 0) {
        	re.enableClientState(IRenderingEngine.RE_NORMAL);
        } else {
        	re.disableClientState(IRenderingEngine.RE_NORMAL);
        }
        re.enableClientState(IRenderingEngine.RE_VERTEX);
    }

    private void setTexCoordPointer(boolean useTexture, int nTexCoord, int type, int stride, int offset, boolean isNative, boolean useBufferManager) {
        if (useTexture) {
        	if (!useBufferManager) {
        		re.setTexCoordPointer(nTexCoord, type, stride, offset);
        	} else if (isNative) {
            	bufferManager.setTexCoordPointer(nativeBufferId, nTexCoord, type, vinfo.vertexSize, offset);
            } else {
            	bufferManager.setTexCoordPointer(bufferId, nTexCoord, type, stride, offset);
            }
        }
    }

    private void setColorPointer(boolean useVertexColor, int nColor, int type, int stride, int offset, boolean isNative, boolean useBufferManager) {
        if (useVertexColor) {
        	if (!useBufferManager) {
        		re.setColorPointer(nColor, type, stride, offset);
        	} else if (isNative) {
                bufferManager.setColorPointer(nativeBufferId, nColor, type, vinfo.vertexSize, offset);
            } else {
            	bufferManager.setColorPointer(bufferId, nColor, type, stride, offset);
            }
        }
    }

    private void setVertexPointer(int nVertex, int type, int stride, int offset, boolean isNative, boolean useBufferManager) {
    	if (!useBufferManager) {
    		re.setVertexPointer(nVertex, type, stride, offset);
    	} else if (isNative) {
            bufferManager.setVertexPointer(nativeBufferId, nVertex, type, vinfo.vertexSize, offset);
        } else {
        	bufferManager.setVertexPointer(bufferId, nVertex, type, stride, offset);
        }
    }

    private void setNormalPointer(int type, int stride, int offset, boolean isNative, boolean useBufferManager) {
        if (vinfo.normal != 0) {
        	if (!useBufferManager) {
        		re.setNormalPointer(type, stride, offset);
        	} else if (isNative) {
                bufferManager.setNormalPointer(nativeBufferId, type, vinfo.vertexSize, offset);
            } else {
            	bufferManager.setNormalPointer(bufferId, type, stride, offset);
            }
        }
    }

    private void setWeightPointer(int numberOfWeightsForBuffer, int type, int stride, int offset, boolean isNative, boolean useBufferManager) {
    	if (numberOfWeightsForBuffer > 0) {
    		if (!useBufferManager) {
    			re.setWeightPointer(numberOfWeightsForBuffer, type, stride, offset);
    		} else if (isNative) {
    			re.setWeightPointer(numberOfWeightsForBuffer, type, vinfo.vertexSize, offset);
    		} else {
    			re.setWeightPointer(numberOfWeightsForBuffer, type, stride, offset);
    		}
    	}
    }

    private void setDataPointers(int nVertex, boolean useVertexColor, int nColor, boolean useTexture, int nTexCoord, boolean useNormal, int numberOfWeightsForBuffer, boolean useBufferManager) {
        int stride = 0, cpos = 0, npos = 0, vpos = 0, wpos = 0;

        if (vinfo.texture != 0 || useTexture) {
            stride += SIZEOF_FLOAT * nTexCoord;
            cpos = npos = vpos = stride;
        }
        if (useVertexColor) {
            stride += SIZEOF_FLOAT * 4;
            npos = vpos = stride;
        }
        if (useNormal) {
            stride += SIZEOF_FLOAT * 3;
            vpos = stride;
        }
        stride += SIZEOF_FLOAT * 3;
        if (numberOfWeightsForBuffer > 0) {
            wpos = stride;
            stride += SIZEOF_FLOAT * numberOfWeightsForBuffer;
        }

        enableClientState(useVertexColor, useTexture);
        setTexCoordPointer(useTexture, nTexCoord, IRenderingEngine.RE_FLOAT, stride, 0, false, useBufferManager);
        setColorPointer(useVertexColor, nColor, IRenderingEngine.RE_FLOAT, stride, cpos, false, useBufferManager);
        setNormalPointer(IRenderingEngine.RE_FLOAT, stride, npos, false, useBufferManager);
        setWeightPointer(numberOfWeightsForBuffer, IRenderingEngine.RE_FLOAT, stride, wpos, false, useBufferManager);
        setVertexPointer(nVertex, IRenderingEngine.RE_FLOAT, stride, vpos, false, useBufferManager);
    }

    public void doPositionSkinning(VertexInfo vinfo, float[] boneWeights, float[] position) {
        float x = 0, y = 0, z = 0;
        for (int i = 0; i < vinfo.skinningWeightCount; i++) {
            if (boneWeights[i] != 0) {
                x += (position[0] * context.bone_uploaded_matrix[i][0]
                        + position[1] * context.bone_uploaded_matrix[i][3]
                        + position[2] * context.bone_uploaded_matrix[i][6]
                        + context.bone_uploaded_matrix[i][9]) * boneWeights[i];

                y += (position[0] * context.bone_uploaded_matrix[i][1]
                        + position[1] * context.bone_uploaded_matrix[i][4]
                        + position[2] * context.bone_uploaded_matrix[i][7]
                        + context.bone_uploaded_matrix[i][10]) * boneWeights[i];

                z += (position[0] * context.bone_uploaded_matrix[i][2]
                        + position[1] * context.bone_uploaded_matrix[i][5]
                        + position[2] * context.bone_uploaded_matrix[i][8]
                        + context.bone_uploaded_matrix[i][11]) * boneWeights[i];
            }
        }

        position[0] = x;
        position[1] = y;
        position[2] = z;
    }

    public void doNormalSkinning(VertexInfo vinfo, float[] boneWeights, float[] normal) {
        float nx = 0, ny = 0, nz = 0;
        for (int i = 0; i < vinfo.skinningWeightCount; i++) {
            if (boneWeights[i] != 0) {
                // Normals shouldn't be translated :)
                nx += (normal[0] * context.bone_uploaded_matrix[i][0]
                        + normal[1] * context.bone_uploaded_matrix[i][3]
                        + normal[2] * context.bone_uploaded_matrix[i][6]) * boneWeights[i];

                ny += (normal[0] * context.bone_uploaded_matrix[i][1]
                        + normal[1] * context.bone_uploaded_matrix[i][4]
                        + normal[2] * context.bone_uploaded_matrix[i][7]) * boneWeights[i];

                nz += (normal[0] * context.bone_uploaded_matrix[i][2]
                        + normal[1] * context.bone_uploaded_matrix[i][5]
                        + normal[2] * context.bone_uploaded_matrix[i][8]) * boneWeights[i];
            }
        }

        /*
        // TODO: I doubt psp hardware normalizes normals after skinning,
        // but if it does, this should be uncommented :)
        float length = nx*nx + ny*ny + nz*nz;

        if (length > 0.f) {
        length = 1.f / (float)Math.sqrt(length);

        nx *= length;
        ny *= length;
        nz *= length;
        }
         */

        normal[0] = nx;
        normal[1] = ny;
        normal[2] = nz;
    }

    private void doSkinning(VertexInfo vinfo, VertexState v) {
        float x = 0, y = 0, z = 0;
        float nx = 0, ny = 0, nz = 0;
        for (int i = 0; i < vinfo.skinningWeightCount; ++i) {
            if (v.boneWeights[i] != 0.f) {

                x += (v.p[0] * context.bone_uploaded_matrix[i][0]
                        + v.p[1] * context.bone_uploaded_matrix[i][3]
                        + v.p[2] * context.bone_uploaded_matrix[i][6]
                        + context.bone_uploaded_matrix[i][9]) * v.boneWeights[i];

                y += (v.p[0] * context.bone_uploaded_matrix[i][1]
                        + v.p[1] * context.bone_uploaded_matrix[i][4]
                        + v.p[2] * context.bone_uploaded_matrix[i][7]
                        + context.bone_uploaded_matrix[i][10]) * v.boneWeights[i];

                z += (v.p[0] * context.bone_uploaded_matrix[i][2]
                        + v.p[1] * context.bone_uploaded_matrix[i][5]
                        + v.p[2] * context.bone_uploaded_matrix[i][8]
                        + context.bone_uploaded_matrix[i][11]) * v.boneWeights[i];

                // Normals shouldn't be translated :)
                nx += (v.n[0] * context.bone_uploaded_matrix[i][0]
                        + v.n[1] * context.bone_uploaded_matrix[i][3]
                        + v.n[2] * context.bone_uploaded_matrix[i][6]) * v.boneWeights[i];

                ny += (v.n[0] * context.bone_uploaded_matrix[i][1]
                        + v.n[1] * context.bone_uploaded_matrix[i][4]
                        + v.n[2] * context.bone_uploaded_matrix[i][7]) * v.boneWeights[i];

                nz += (v.n[0] * context.bone_uploaded_matrix[i][2]
                        + v.n[1] * context.bone_uploaded_matrix[i][5]
                        + v.n[2] * context.bone_uploaded_matrix[i][8]) * v.boneWeights[i];
            }
        }

        v.p[0] = x;
        v.p[1] = y;
        v.p[2] = z;

        /*
        // TODO: I doubt psp hardware normalizes normals after skinning,
        // but if it does, this should be uncommented :)
        float length = nx*nx + ny*ny + nz*nz;

        if (length > 0.f) {
        length = 1.f / (float)Math.sqrt(length);

        nx *= length;
        ny *= length;
        nz *= length;
        }
         */

        v.n[0] = nx;
        v.n[1] = ny;
        v.n[2] = nz;
    }

    private void log(String commandString, float floatArgument) {
        if (isLogDebugEnabled) {
            log(commandString + SPACE + floatArgument);
        }
    }

    private void log(String commandString, int value) {
        if (isLogDebugEnabled) {
            log(commandString + SPACE + value);
        }
    }

    private void log(String commandString, float[] matrix) {
        if (isLogDebugEnabled) {
            for (int y = 0; y < 4; y++) {
                log(commandString + SPACE + String.format("%.1f %.1f %.1f %.1f", matrix[0 + y * 4], matrix[1 + y * 4], matrix[2 + y * 4], matrix[3 + y * 4]));
            }
        }
    }

    public boolean isVideoTexture(int tex_addr) {
    	if (!videoTextures.isEmpty()) {
    		// Synchronize the access to videoTextures as it can be accessed
    		// from a parallel threads (async display and PSP thread)
    		synchronized (videoTextures) {
        		for (AddressRange addressRange : videoTextures) {
        			if (addressRange.contains(tex_addr)) {
        				return true;
        			}
        		}
			}
    	}

    	return false;
    }

    private boolean canCacheTexture(int tex_addr) {
    	if (!useTextureCache) {
    		return false;
    	}

        // Some games are storing compressed textures in VRAM (e.g. Skate Park City).
    	if (context.texture_storage >= TPSM_PIXEL_STORAGE_MODE_DXT1) {
    		return true;
    	}

    	if (isVRAM(tex_addr)) {
            // Force only a reload of textures that can be generated by the GE buffer,
            // i.e. when texture_storage is one of
            // BGR5650=0, ABGR5551=1, ABGR4444=2 or ABGR8888=3.
    		if (display.getSaveGEToTexture() || context.texture_storage <= TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
    			return false;
    		}
        }

    	if (isVideoTexture(tex_addr)) {
    		return false;
    	}

    	return true;
    }

    private void setFlippedTexture(GETexture geTexture) {
		// Textures are normally created as flipped textures (upside-down)
		// but the GE textures are not flipped, so we have to flip
		// them when rendering
		float newTextureFlipTranslateY = geTexture.getHeight() / (float) context.texture_height[0];
		if (!textureFlipped || textureFlipTranslateY != newTextureFlipTranslateY) {
			textureFlipped = true;
			textureFlipTranslateY = newTextureFlipTranslateY;
			textureMatrixUpload.setChanged(true);
		}
    }

    private boolean loadGETexture(int tex_addr) {
    	if (!display.getSaveGEToTexture() || isVideoTexture(tex_addr)) {
    		return false;
    	}

		int widthGe = display.getWidthGe();
		int heightGe = display.getHeightGe();
		int bufferWidth = context.texture_buffer_width[0];
		int pixelFormat = context.texture_storage;

		GETexture geTexture;
		if (IRenderingEngine.isTextureTypeIndexed[pixelFormat]) {
			if (pixelFormat == TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED) {
				geTexture = GETextureManager.getInstance().checkGETexture(tex_addr, bufferWidth, widthGe, heightGe, TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888);
			} else {
				// We only know that the texture is 16-bit indexed, but we don't
				// know its exact pixel format (5650, 5551 or 4444)
				geTexture = null;

				if (context.tex_clut_mode >= TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650 && context.tex_clut_mode <= TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444) {
					// First try with the same pixel format as the texture clut
					geTexture = GETextureManager.getInstance().checkGETexture(tex_addr, bufferWidth, widthGe, heightGe, context.tex_clut_mode);
				}

				if (geTexture == null) {
					// As a last chance, try all the pixel formats: 5650, 5551, 4444
					for (int i = TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650; i <= TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444; i++) {
						geTexture = GETextureManager.getInstance().checkGETexture(tex_addr, bufferWidth, widthGe, heightGe, i);
						if (geTexture != null) {
							break;
						}
					}
				}
			}

			if (geTexture != null) {
				if (tex_addr == display.getTopAddrGe()) {
					// Using the active GE as texture
					geTexture.copyScreenToTexture(re);
				}

				if (!re.canNativeClut() || context.texture_swizzle) {
					// Save the texture to memory, it will be reloaded using the CLUT
					geTexture.copyTextureToMemory(re);
					return false;
				}

				geTexture = GETextureManager.getInstance().getGEIndexedTexture(re, geTexture, tex_addr, bufferWidth, context.texture_width[0], context.texture_height[0], pixelFormat);
				geTexture.bind(re, true);
				setFlippedTexture(geTexture);

				return true;
			}
		} else {
			geTexture = GETextureManager.getInstance().checkGETexture(tex_addr, bufferWidth, widthGe, heightGe, pixelFormat);
		}

		if (geTexture == null) {
			return false;
		}

		if (tex_addr == display.getTopAddrGe()) {
			// Using the active GE as texture
			geTexture.copyScreenToTexture(re);
		}

		int width = context.texture_width[0];
		int height = context.texture_height[0];
		if (geTexture.getWidth() == width && geTexture.getHeight() == height) {
			if (isLogDebugEnabled) {
				log.debug(String.format("Reusing GETexture %s", geTexture));
			}
		} else {
			// Resize the GETexture to the requested texture size
			if (isLogDebugEnabled) {
				log.debug(String.format("Resizing GETexture %s to %dx%d", geTexture, width, height));
			}
			geTexture = GETextureManager.getInstance().getGEResizedTexture(re, geTexture, tex_addr, bufferWidth, width, height, pixelFormat);
		}
		geTexture.bind(re, true);
		setFlippedTexture(geTexture);

		return true;
    }

    private int getValidNumberMipmaps() {
    	for (int level = 0; level <= context.texture_num_mip_maps; level++) {
    		int texaddr = context.texture_base_pointer[level] & Memory.addressMask;
    		if (!Memory.isAddressGood(texaddr)) {
            	if (texaddr == 0) {
            		if (isLogDebugEnabled) {
            			log.debug(String.format("Invalid texture address 0x%08X for texture level %d", texaddr, level));
            		}
            	} else {
            		if (isLogWarnEnabled) {
            			log.warn(String.format("Invalid texture address 0x%08X for texture level %d", texaddr, level));
            		}
            	}
    			return Math.max(level - 1, 0);
    		}

    		if (level > 0) {
            	int previousWidth = context.texture_width[level - 1];
            	int currentWidth = context.texture_width[level];
            	int previousHeight = context.texture_height[level - 1];
            	int currentHeight = context.texture_height[level];
            	if (currentWidth * 2 != previousWidth || currentHeight * 2 != previousHeight) {
            		if (isLogWarnEnabled) {
            			log.warn(String.format("Texture mipmap with invalid dimension at level %d: (%dx%d)@0x%08X -> (%dx%d)@0x%08X", level, previousWidth, previousHeight, context.texture_base_pointer[level - 1] & Memory.addressMask, currentWidth, currentHeight, texaddr));
            			if (context.tex_mipmap_mode == TBIAS_MODE_CONST && context.tex_mipmap_bias_int >= level) {
                			log.warn(String.format("... and this invalid Texture mipmap will be used with mipmap_mode=%d, mipmap_bias=%d", context.tex_mipmap_mode, context.tex_mipmap_bias_int));
            			}
            		}
            		return level - 1;
            	}
            }
    	}

    	return context.texture_num_mip_maps;
    }

    private void loadTexture() {
        // No need to reload or check the texture cache if no texture parameter
        // has been changed since last call loadTexture()
        if (!textureChanged) {
            return;
        }

        // HACK: avoid texture uploads of null pointers
        // This can come from Sony's GE init code (pspsdk GE init is ok)
        if (context.texture_base_pointer[0] == 0) {
            return;
        }

        // Texture not used when disabled (automatically disabled in clear mode).
        if (!context.textureFlag.isEnabled()) {
            return;
        }

        int tex_addr = context.texture_base_pointer[0] & Memory.addressMask;
        if (!Memory.isAddressGood(tex_addr)) {
        	if (isLogWarnEnabled) {
        		log.warn(String.format("Invalid texture address 0x%08X for texture level 0", tex_addr));
        	}
    		return;
        }

        re.setTextureFormat(context.texture_storage, context.texture_swizzle);

        Texture texture;
		if (!canCacheTexture(tex_addr)) {
        	if (loadGETexture(tex_addr)) {
                re.setTextureMipmapMagFilter(context.tex_mag_filter);
                re.setTextureMipmapMinFilter(context.tex_min_filter);
                checkTextureMinFilter(false, context.texture_num_mip_maps);
        		textureChanged = false;
        		return;
        	}

        	texture = null;

            // Generate a texture id if we don't have one
            if (textureId == -1) {
            	textureId = re.genTexture();
            }

            re.bindTexture(textureId);
        } else {
            TextureCache textureCache = TextureCache.getInstance();
            boolean textureRequiresClut = IRenderingEngine.isTextureTypeIndexed[context.texture_storage];
        	if (textureRequiresClut && re.canNativeClut()) {
        		if (context.texture_storage >= TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED && context.texture_storage <= TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED) {
        			// The Clut will be resolved by the shader
        			textureRequiresClut = false;
        		}
        	}

        	textureCacheLookupStatistics.start();
            // Check if the texture is in the cache
            if (textureRequiresClut) {
            	texture = textureCache.getTexture(context.texture_base_pointer[0], context.texture_buffer_width[0], context.texture_width[0], context.texture_height[0], context.texture_storage, context.tex_clut_addr, context.tex_clut_mode, context.tex_clut_start, context.tex_clut_shift, context.tex_clut_mask, context.tex_clut_num_blocks, context.texture_num_mip_maps, context.mipmapShareClut, null, null);
            } else {
            	texture = textureCache.getTexture(context.texture_base_pointer[0], context.texture_buffer_width[0], context.texture_width[0], context.texture_height[0], context.texture_storage, 0, 0, 0, 0, 0, 0, context.texture_num_mip_maps, false, null, null);
            }
        	textureCacheLookupStatistics.end();

            // Create the texture if not yet in the cache
            if (texture == null) {
            	if (textureRequiresClut) {
            		texture = new Texture(textureCache, context.texture_base_pointer[0], context.texture_buffer_width[0], context.texture_width[0], context.texture_height[0], context.texture_storage, context.tex_clut_addr, context.tex_clut_mode, context.tex_clut_start, context.tex_clut_shift, context.tex_clut_mask, context.tex_clut_num_blocks, context.texture_num_mip_maps, context.mipmapShareClut, null, null);
            	} else {
            		texture = new Texture(textureCache, context.texture_base_pointer[0], context.texture_buffer_width[0], context.texture_width[0], context.texture_height[0], context.texture_storage, 0, 0, 0, 0, 0, 0, context.texture_num_mip_maps, false, null, null);
            	}
                textureCache.addTexture(re, texture);
            }

            texture.bindTexture(re);
        }

        if (textureFlipped) {
        	textureFlipped = false;
        	textureMatrixUpload.setChanged(true);
        }

        // Load the texture if not yet loaded
        if (texture == null || !texture.isLoaded() || State.captureGeNextFrame || State.replayGeNextFrame) {
            if (isLogDebugEnabled) {
                log(helper.getCommandString(TFLUSH)
                        + " " + String.format("0x%08X", context.texture_base_pointer[0])
                        + ", buffer_width=" + context.texture_buffer_width[0]
                        + " (" + context.texture_width[0] + "," + context.texture_height[0] + ")");

                log(helper.getCommandString(TFLUSH)
                        + " texture_storage=0x" + Integer.toHexString(context.texture_storage)
                        + "(" + getPsmName(context.texture_storage)
                        + "), tex_clut_mode=0x" + Integer.toHexString(context.tex_clut_mode)
                        + ", tex_clut_addr=" + String.format("0x%08X", context.tex_clut_addr)
                        + ", texture_swizzle=" + context.texture_swizzle);
            }

            Buffer final_buffer = null;
            int texclut = context.tex_clut_addr;
            int texaddr;

            int textureByteAlignment = 4;   // 32 bits
            boolean compressedTexture = false;

            int numberMipmaps = getValidNumberMipmaps();

            // Set the texture min/mag filters before uploading the texture
            // (some drivers have problems changing the parameters afterwards)
            re.setTextureMipmapMagFilter(context.tex_mag_filter);
            re.setTextureMipmapMinFilter(context.tex_min_filter);
            checkTextureMinFilter(compressedTexture, numberMipmaps);

            for (int level = 0; level <= numberMipmaps; level++) {
                // Extract texture information with the minor conversion possible
                // TODO: Get rid of information copying, and implement all the available formats
                texaddr = context.texture_base_pointer[level] & Memory.addressMask;
                compressedTexture = false;
                int compressedTextureSize = 0;
                int buffer_storage = context.texture_storage;
                int textureBufferWidthInPixels = context.texture_buffer_width[level];

                switch (context.texture_storage) {
                    case TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED: {
                        if (texclut == 0) {
                            return;
                        }

                        buffer_storage = context.tex_clut_mode;
                        switch (context.tex_clut_mode) {
                            case CMODE_FORMAT_16BIT_BGR5650:
                            case CMODE_FORMAT_16BIT_ABGR5551:
                            case CMODE_FORMAT_16BIT_ABGR4444: {
                                textureByteAlignment = 2;  // 16 bits
                                short[] clut = readClut16(level);
                                int clutSharingOffset = context.mipmapShareClut ? 0 : level * 16;

                                if (!context.texture_swizzle) {
                                    // In case of 4-bit indexed, texture_buffer_width is the size in bytes,
                                    // not in pixels.
                                    textureBufferWidthInPixels *= 2;

                                    int length = Math.max(textureBufferWidthInPixels, context.texture_width[level]) * context.texture_height[level];
                                    IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, length / 2, 1);
                                    for (int i = 0; i < length; i += 2) {
                                        int index = memoryReader.readNext();

                                        tmp_texture_buffer16[i] = clut[getClutIndex(index & 0xF) + clutSharingOffset];
                                        tmp_texture_buffer16[i + 1] = clut[getClutIndex((index >> 4) & 0xF) + clutSharingOffset];
                                    }
                                    final_buffer = ShortBuffer.wrap(tmp_texture_buffer16);

                                    if (State.captureGeNextFrame) {
                                        log.info("Capture loadTexture clut 4/16 unswizzled");
                                        CaptureManager.captureRAM(texaddr, length / 2);
                                    }
                                } else {
                                    unswizzleTextureFromMemory(texaddr, 0, level);
                                    int pixels = context.texture_buffer_width[level] * context.texture_height[level];
                                    for (int i = 0, j = 0; i < pixels; i += 8, j++) {
                                        int n = tmp_texture_buffer32[j];
                                        int index = n & 0xF;
                                        tmp_texture_buffer16[i + 0] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 4) & 0xF;
                                        tmp_texture_buffer16[i + 1] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 8) & 0xF;
                                        tmp_texture_buffer16[i + 2] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 12) & 0xF;
                                        tmp_texture_buffer16[i + 3] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 16) & 0xF;
                                        tmp_texture_buffer16[i + 4] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 20) & 0xF;
                                        tmp_texture_buffer16[i + 5] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 24) & 0xF;
                                        tmp_texture_buffer16[i + 6] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 28) & 0xF;
                                        tmp_texture_buffer16[i + 7] = clut[getClutIndex(index) + clutSharingOffset];
                                    }
                                    final_buffer = ShortBuffer.wrap(tmp_texture_buffer16);
                                    break;
                                }

                                break;
                            }

                            case CMODE_FORMAT_32BIT_ABGR8888: {
                                int[] clut = readClut32(level);
                                int clutSharingOffset = context.mipmapShareClut ? 0 : level * 16;

                                if (!context.texture_swizzle) {
                                    // In case of 4-bit indexed, texture_buffer_width is the size in bytes,
                                    // not in pixels.
                                    textureBufferWidthInPixels *= 2;

                                    int length = Math.max(textureBufferWidthInPixels, context.texture_width[level]) * context.texture_height[level];
                                    IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, length / 2, 1);
                                    for (int i = 0; i < length; i += 2) {
                                        int index = memoryReader.readNext();

                                        tmp_texture_buffer32[i + 1] = clut[getClutIndex((index >> 4) & 0xF) + clutSharingOffset];
                                        tmp_texture_buffer32[i] = clut[getClutIndex(index & 0xF) + clutSharingOffset];
                                    }
                                    final_buffer = IntBuffer.wrap(tmp_texture_buffer32);

                                    if (State.captureGeNextFrame) {
                                        log.info("Capture loadTexture clut 4/32 unswizzled");
                                        CaptureManager.captureRAM(texaddr, length / 2);
                                    }
                                } else {
                                    unswizzleTextureFromMemory(texaddr, 0, level);
                                    int pixels = context.texture_buffer_width[level] * context.texture_height[level];
                                    for (int i = pixels - 8, j = (pixels / 8) - 1; i >= 0; i -= 8, j--) {
                                        int n = tmp_texture_buffer32[j];
                                        int index = n & 0xF;
                                        tmp_texture_buffer32[i + 0] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 4) & 0xF;
                                        tmp_texture_buffer32[i + 1] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 8) & 0xF;
                                        tmp_texture_buffer32[i + 2] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 12) & 0xF;
                                        tmp_texture_buffer32[i + 3] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 16) & 0xF;
                                        tmp_texture_buffer32[i + 4] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 20) & 0xF;
                                        tmp_texture_buffer32[i + 5] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 24) & 0xF;
                                        tmp_texture_buffer32[i + 6] = clut[getClutIndex(index) + clutSharingOffset];
                                        index = (n >> 28) & 0xF;
                                        tmp_texture_buffer32[i + 7] = clut[getClutIndex(index) + clutSharingOffset];
                                    }
                                    final_buffer = IntBuffer.wrap(tmp_texture_buffer32);
                                }

                                break;
                            }

                            default: {
                                error("Unhandled clut4 texture mode " + context.tex_clut_mode);
                                return;
                            }
                        }

                        break;
                    }
                    case TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED: {
                    	if (re.canNativeClut()) {
                            final_buffer = getTextureBuffer(texaddr, 1, level);
                            textureByteAlignment = 1; // 8 bits
                    	} else {
	                        final_buffer = readIndexedTexture(level, texaddr, texclut, 1);
	                        buffer_storage = context.tex_clut_mode;
	                        textureByteAlignment = textureByteAlignmentMapping[context.tex_clut_mode];
                    	}
                        break;
                    }
                    case TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED: {
                    	if (re.canNativeClut()) {
                    		final_buffer = getTextureBuffer(texaddr, 2, level);
                    		textureByteAlignment = 2; // 16 bits
                    	} else {
                    		final_buffer = readIndexedTexture(level, texaddr, texclut, 2);
                    		buffer_storage = context.tex_clut_mode;
                    		textureByteAlignment = textureByteAlignmentMapping[context.tex_clut_mode];
                    	}
                        break;
                    }
                    case TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED: {
                    	if (re.canNativeClut()) {
                    		final_buffer = getTextureBuffer(texaddr, 4, level);
                    		textureByteAlignment = 4; // 32 bits
                    	} else {
	                        final_buffer = readIndexedTexture(level, texaddr, texclut, 4);
	                        buffer_storage = context.tex_clut_mode;
	                        textureByteAlignment = textureByteAlignmentMapping[context.tex_clut_mode];
                    	}
                        break;
                    }
                    case TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650:
                    case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551:
                    case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444: {
                        textureByteAlignment = 2;  // 16 bits

                        if (!context.texture_swizzle) {
                            int length = Math.max(context.texture_buffer_width[level], context.texture_width[level]) * context.texture_height[level];
                            final_buffer = Memory.getInstance().getBuffer(texaddr, length * 2);
                            if (final_buffer == null) {
                                IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, length * 2, 2);
                                for (int i = 0; i < length; i++) {
                                    int pixel = memoryReader.readNext();
                                    tmp_texture_buffer16[i] = (short) pixel;
                                }

                                final_buffer = ShortBuffer.wrap(tmp_texture_buffer16);
                            }

                            if (State.captureGeNextFrame) {
                                log.info("Capture loadTexture 16 unswizzled");
                                CaptureManager.captureRAM(texaddr, length * 2);
                            }
                        } else {
                            final_buffer = unswizzleTextureFromMemory(texaddr, 2, level);
                        }

                        break;
                    }

                    case TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888: {
                        final_buffer = getTextureBuffer(texaddr, 4, level);
                        break;
                    }

                    case TPSM_PIXEL_STORAGE_MODE_DXT1: {
                        if (isLogDebugEnabled) {
                            log.debug("Loading texture TPSM_PIXEL_STORAGE_MODE_DXT1 " + Integer.toHexString(texaddr));
                        }
                        compressedTexture = true;
                        compressedTextureSize = getCompressedTextureSize(level, 8);
                        IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, compressedTextureSize, 4);
                        // PSP DXT1 hardware format reverses the colors and the per-pixel
                        // bits, and encodes the color in RGB 565 format
                        int i = 0;
                        for (int y = 0; y < context.texture_height[level]; y += 4) {
                            for (int x = 0; x < context.texture_buffer_width[level]; x += 4, i += 2) {
                                tmp_texture_buffer32[i + 1] = memoryReader.readNext();
                                tmp_texture_buffer32[i + 0] = memoryReader.readNext();
                            }
                            for (int x = context.texture_buffer_width[level]; x < context.texture_width[level]; x += 4, i += 2) {
                                tmp_texture_buffer32[i + 0] = 0;
                                tmp_texture_buffer32[i + 1] = 0;
                            }
                        }
                        final_buffer = IntBuffer.wrap(tmp_texture_buffer32);
                        break;
                    }

                    case TPSM_PIXEL_STORAGE_MODE_DXT3: {
                        if (isLogDebugEnabled) {
                            log.debug("Loading texture TPSM_PIXEL_STORAGE_MODE_DXT3 " + Integer.toHexString(texaddr));
                        }
                        compressedTexture = true;
                        compressedTextureSize = getCompressedTextureSize(level, 4);
                        IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, compressedTextureSize, 4);
                        // PSP DXT3 format reverses the alpha and color parts of each block,
                        // and reverses the color and per-pixel terms in the color part.
                        int i = 0;
                        for (int y = 0; y < context.texture_height[level]; y += 4) {
                            for (int x = 0; x < context.texture_buffer_width[level]; x += 4, i += 4) {
                                // Color
                                tmp_texture_buffer32[i + 3] = memoryReader.readNext();
                                tmp_texture_buffer32[i + 2] = memoryReader.readNext();
                                // Alpha
                                tmp_texture_buffer32[i + 0] = memoryReader.readNext();
                                tmp_texture_buffer32[i + 1] = memoryReader.readNext();
                            }
                            for (int x = context.texture_buffer_width[level]; x < context.texture_width[level]; x += 4, i += 4) {
                                tmp_texture_buffer32[i + 0] = 0;
                                tmp_texture_buffer32[i + 1] = 0;
                                tmp_texture_buffer32[i + 2] = 0;
                                tmp_texture_buffer32[i + 3] = 0;
                            }
                        }
                        final_buffer = IntBuffer.wrap(tmp_texture_buffer32);
                        break;
                    }

                    case TPSM_PIXEL_STORAGE_MODE_DXT5: {
                        if (isLogDebugEnabled) {
                            log.debug("Loading texture TPSM_PIXEL_STORAGE_MODE_DXT5 " + Integer.toHexString(texaddr));
                        }
                        compressedTexture = true;
                        compressedTextureSize = getCompressedTextureSize(level, 4);
                        IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, compressedTextureSize, 2);
                        // PSP DXT5 format reverses the alpha and color parts of each block,
                        // and reverses the color and per-pixel terms in the color part. In
                        // the alpha part, the 2 reference alpha values are swapped with the
                        // alpha interpolation values.
                        int i = 0;
                        for (int y = 0; y < context.texture_height[level]; y += 4) {
                            for (int x = 0; x < context.texture_buffer_width[level]; x += 4, i += 8) {
                                // Color
                                tmp_texture_buffer16[i + 6] = (short) memoryReader.readNext();
                                tmp_texture_buffer16[i + 7] = (short) memoryReader.readNext();
                                tmp_texture_buffer16[i + 4] = (short) memoryReader.readNext();
                                tmp_texture_buffer16[i + 5] = (short) memoryReader.readNext();
                                // Alpha
                                tmp_texture_buffer16[i + 1] = (short) memoryReader.readNext();
                                tmp_texture_buffer16[i + 2] = (short) memoryReader.readNext();
                                tmp_texture_buffer16[i + 3] = (short) memoryReader.readNext();
                                tmp_texture_buffer16[i + 0] = (short) memoryReader.readNext();
                            }
                            for (int x = context.texture_buffer_width[level]; x < context.texture_width[level]; x += 4, i += 8) {
                                tmp_texture_buffer16[i + 0] = 0;
                                tmp_texture_buffer16[i + 1] = 0;
                                tmp_texture_buffer16[i + 2] = 0;
                                tmp_texture_buffer16[i + 3] = 0;
                                tmp_texture_buffer16[i + 4] = 0;
                                tmp_texture_buffer16[i + 5] = 0;
                                tmp_texture_buffer16[i + 6] = 0;
                                tmp_texture_buffer16[i + 7] = 0;
                            }
                        }
                        final_buffer = ShortBuffer.wrap(tmp_texture_buffer16);
                        break;
                    }

                    default: {
                        error("Unhandled texture storage " + context.texture_storage);
                        return;
                    }
                }

                // Upload texture to openGL.
                re.setPixelStore(textureBufferWidthInPixels, textureByteAlignment);

                if (compressedTexture) {
                    re.setCompressedTexImage(
                            level,
                            buffer_storage,
                            context.texture_width[level], context.texture_height[level],
                            compressedTextureSize,
                            final_buffer);
                } else {
                	int textureSize = Math.max(textureBufferWidthInPixels, context.texture_width[level]) * context.texture_height[level] * textureByteAlignment;
                    re.setTexImage(
                            level,
                            buffer_storage,
                            context.texture_width[level], context.texture_height[level],
                            buffer_storage,
                            buffer_storage,
                            textureSize,
                            final_buffer);
                }

                if (State.captureGeNextFrame) {
                	boolean vramImage = isVRAM(tex_addr);
                	boolean overwriteFile = !vramImage;
                    if (vramImage || !CaptureManager.isImageCaptured(texaddr)) {
                        CaptureManager.captureImage(texaddr, level, final_buffer, context.texture_width[level], context.texture_height[level], context.texture_buffer_width[level], buffer_storage, compressedTexture, compressedTextureSize, false, overwriteFile);
                    }
                }

                if (texture != null) {
                    texture.setIsLoaded();
                    if (isLogDebugEnabled) {
                        log(helper.getCommandString(TFLUSH) + " Loaded texture " + texture.getGlId());
                    }
                }
            }
        } else {
            boolean compressedTexture = (context.texture_storage >= TPSM_PIXEL_STORAGE_MODE_DXT1 && context.texture_storage <= TPSM_PIXEL_STORAGE_MODE_DXT5);
            re.setTextureMipmapMagFilter(context.tex_mag_filter);
            re.setTextureMipmapMinFilter(context.tex_min_filter);
            checkTextureMinFilter(compressedTexture, context.texture_num_mip_maps);

            if (isLogDebugEnabled) {
                log(helper.getCommandString(TFLUSH) + " Reusing cached texture " + texture.getGlId());
            }
        }

        textureChanged = false;
    }

    private void checkTextureMinFilter(boolean compressedTexture, int numberMipmaps) {
        // OpenGL/Hardware cannot interpolate between compressed textures;
        // this restriction has been checked on NVIDIA GeForce 8500 GT and 9800 GT
        if (compressedTexture) {
            int new_tex_min_filter;
            if (context.tex_min_filter == TFLT_NEAREST || context.tex_min_filter == TFLT_NEAREST_MIPMAP_LINEAR || context.tex_min_filter == TFLT_NEAREST_MIPMAP_NEAREST) {
                new_tex_min_filter = TFLT_NEAREST;
            } else {
                new_tex_min_filter = TFLT_LINEAR;
            }

            if (new_tex_min_filter != context.tex_min_filter) {
	            re.setTextureMipmapMinFilter(new_tex_min_filter);
	            if (isLogDebugEnabled) {
	                log("Overwriting texture min filter, no mipmap was generated but filter was set to use mipmap");
	            }
            }
        }
    }

    private Buffer readIndexedTexture(int level, int texaddr, int texclut, int bytesPerIndex) {
        Buffer buffer = null;

        int length = context.texture_buffer_width[level] * context.texture_height[level];
        switch (context.tex_clut_mode) {
            case CMODE_FORMAT_16BIT_BGR5650:
            case CMODE_FORMAT_16BIT_ABGR5551:
            case CMODE_FORMAT_16BIT_ABGR4444: {
                if (texclut == 0) {
                    return null;
                }

                short[] clut = readClut16(level);

                if (!context.texture_swizzle) {
                    IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, length * bytesPerIndex, bytesPerIndex);
                    for (int i = 0; i < length; i++) {
                        int index = memoryReader.readNext();
                        tmp_texture_buffer16[i] = clut[getClutIndex(index)];
                    }
                    buffer = ShortBuffer.wrap(tmp_texture_buffer16);

                    if (State.captureGeNextFrame) {
                        log.info("Capture loadTexture clut 8/16 unswizzled");
                        CaptureManager.captureRAM(texaddr, length * bytesPerIndex);
                    }
                } else {
                    unswizzleTextureFromMemory(texaddr, bytesPerIndex, level);
                    switch (bytesPerIndex) {
                        case 1: {
                            for (int i = 0, j = 0; i < length; i += 4, j++) {
                                int n = tmp_texture_buffer32[j];
                                int index = n & 0xFF;
                                tmp_texture_buffer16[i + 0] = clut[getClutIndex(index)];
                                index = (n >> 8) & 0xFF;
                                tmp_texture_buffer16[i + 1] = clut[getClutIndex(index)];
                                index = (n >> 16) & 0xFF;
                                tmp_texture_buffer16[i + 2] = clut[getClutIndex(index)];
                                index = (n >> 24) & 0xFF;
                                tmp_texture_buffer16[i + 3] = clut[getClutIndex(index)];
                            }
                            break;
                        }
                        case 2: {
                            for (int i = 0, j = 0; i < length; i += 2, j++) {
                                int n = tmp_texture_buffer32[j];
                                tmp_texture_buffer16[i + 0] = clut[getClutIndex(n & 0xFFFF)];
                                tmp_texture_buffer16[i + 1] = clut[getClutIndex(n >>> 16)];
                            }
                            break;
                        }
                        case 4: {
                            for (int i = 0; i < length; i++) {
                                int n = tmp_texture_buffer32[i];
                                tmp_texture_buffer16[i] = clut[getClutIndex(n)];
                            }
                            break;
                        }
                    }
                    buffer = ShortBuffer.wrap(tmp_texture_buffer16);
                }

                break;
            }

            case CMODE_FORMAT_32BIT_ABGR8888: {
                if (texclut == 0) {
                    return null;
                }

                int[] clut = readClut32(level);

                if (!context.texture_swizzle) {
                    IMemoryReader memoryReader = MemoryReader.getMemoryReader(texaddr, length * bytesPerIndex, bytesPerIndex);
                    for (int i = 0; i < length; i++) {
                        int index = memoryReader.readNext();
                        tmp_texture_buffer32[i] = clut[getClutIndex(index)];
                    }
                    buffer = IntBuffer.wrap(tmp_texture_buffer32);

                    if (State.captureGeNextFrame) {
                        log.info("Capture loadTexture clut 8/32 unswizzled");
                        CaptureManager.captureRAM(texaddr, length * bytesPerIndex);
                    }
                } else {
                    unswizzleTextureFromMemory(texaddr, bytesPerIndex, level);
                    switch (bytesPerIndex) {
                        case 1: {
                            for (int i = length - 4, j = (length / 4) - 1; i >= 0; i -= 4, j--) {
                                int n = tmp_texture_buffer32[j];
                                int index = n & 0xFF;
                                tmp_texture_buffer32[i + 0] = clut[getClutIndex(index)];
                                index = (n >> 8) & 0xFF;
                                tmp_texture_buffer32[i + 1] = clut[getClutIndex(index)];
                                index = (n >> 16) & 0xFF;
                                tmp_texture_buffer32[i + 2] = clut[getClutIndex(index)];
                                index = (n >> 24) & 0xFF;
                                tmp_texture_buffer32[i + 3] = clut[getClutIndex(index)];
                            }
                            break;
                        }
                        case 2: {
                            for (int i = length - 2, j = (length / 2) - 1; i >= 0; i -= 2, j--) {
                                int n = tmp_texture_buffer32[j];
                                tmp_texture_buffer32[i + 0] = clut[getClutIndex(n & 0xFFFF)];
                                tmp_texture_buffer32[i + 1] = clut[getClutIndex(n >>> 16)];
                            }
                            break;
                        }
                        case 4: {
                            for (int i = 0; i < length; i++) {
                                int n = tmp_texture_buffer32[i];
                                tmp_texture_buffer32[i] = clut[getClutIndex(n)];
                            }
                            break;
                        }
                    }
                    buffer = IntBuffer.wrap(tmp_texture_buffer32);
                }

                break;
            }

            default: {
                error("Unhandled clut8 texture mode " + context.tex_clut_mode);
                break;
            }
        }

        return buffer;
    }

    private void setScissor() {
        if (context.scissor_x1 >= 0 && context.scissor_y1 >= 0
                && context.scissor_width <= context.region_width
                && context.scissor_height <= context.region_height) {
        	int scissorX = context.scissor_x1;
        	int scissorY = context.scissor_y1;
        	int scissorWidth = context.scissor_width ;
        	int scissorHeight = context.scissor_height;

        	if (scissorHeight < Screen.height) {
        		scissorY = Screen.height - scissorHeight - scissorY;
        	}

            re.setScissor(scissorX, scissorY, scissorWidth, scissorHeight);
        	context.scissorTestFlag.setEnabled(true);
        } else {
        	context.scissorTestFlag.setEnabled(false);
        }
    }

    private float[] getProjectionMatrix() {
    	if (context.transform_mode == VTYPE_TRANSFORM_PIPELINE_RAW_COORD) {
    		// 2D
    		return null;
    	}

    	if (context.viewport_height <= 0 && context.viewport_width >= 0) {
    		// Non-flipped 3D
    		return context.proj_uploaded_matrix;
    	}

    	float[] flippedMatrix = new float[16];
		System.arraycopy(context.proj_uploaded_matrix, 0, flippedMatrix, 0, flippedMatrix.length);
		if (context.viewport_height > 0) {
    		// Flip upside-down
			flippedMatrix[5] = -flippedMatrix[5];
			flippedMatrix[13] = -flippedMatrix[13];
		}
		if (context.viewport_width < 0) {
			// Flip right-to-left
			flippedMatrix[0] = -flippedMatrix[0];
			flippedMatrix[12] = -flippedMatrix[12];
		}

		return flippedMatrix;
    }

    private boolean initRendering() {
        /*
         * Defer transformations until primitive rendering
         */

    	/*
    	 * Set Scissor
    	 */
    	if (scissorChanged) {
    		setScissor();
    		scissorChanged = false;
    	}

        /*
         * Apply projection matrix
         */
        if (projectionMatrixUpload.isChanged()) {
        	re.setProjectionMatrix(getProjectionMatrix());
            projectionMatrixUpload.setChanged(false);

            // The viewport has to be reloaded when the projection matrix has changed
            viewportChanged = true;
        }

        /*
         * Apply viewport
         */
        boolean loadOrtho2D = false;
        if (viewportChanged) {
            if (context.transform_mode == VTYPE_TRANSFORM_PIPELINE_RAW_COORD) {
                re.setViewport(0, 0, Screen.width, Screen.height);
                // Load the ortho for 2D after the depth settings
                loadOrtho2D = true;
            } else {
                int halfHeight = Math.abs(context.viewport_height);
                int halfWidth = Math.abs(context.viewport_width);
                int viewportX = context.viewport_cx - halfWidth - context.offset_x;
                int viewportY = context.viewport_cy - halfHeight - context.offset_y;
                int viewportWidth = 2 * halfWidth;
                int viewportHeight = 2 * halfHeight;

                // For OpenGL, translate the viewportY from the upper left corner
                // to the lower left corner.
                viewportY = Screen.height - viewportY - viewportHeight;

                re.setViewport(viewportX, viewportY, viewportWidth, viewportHeight);
            }
            viewportChanged = false;
        }

        /*
         * Apply depth handling
         */
        if (depthChanged) {
            if (context.transform_mode == VTYPE_TRANSFORM_PIPELINE_TRANS_COORD) {
            	re.setDepthFunc(context.depthFunc);
                re.setDepthRange(context.zpos, context.zscale, context.zpos - context.zscale, context.zpos + context.zscale);
            } else {
            	re.setDepthFunc(context.depthFunc);
                re.setDepthRange(0.5f, 0.5f, 0, 1);
            }
            depthChanged = false;
        }

        /*
         * Load the 2D ortho (only after the depth settings
         */
        if (loadOrtho2D) {
            re.setProjectionMatrix(getOrthoMatrix(0, 480, 272, 0, 0, -0xFFFF));
        }

        /*
         * 2D mode handling
         */
        if (context.transform_mode == VTYPE_TRANSFORM_PIPELINE_RAW_COORD) {
            // 2D mode shouldn't be affected by the lighting and fog
        	re.disableFlag(IRenderingEngine.GU_LIGHTING);
        	re.disableFlag(IRenderingEngine.GU_FOG);

            // TODO I don't know why, but the GL_MODELVIEW matrix has to be reloaded
            // each time in 2D mode... Otherwise textures are not displayed.
            modelMatrixUpload.setChanged(true);
        } else {
        	context.lightingFlag.update();
        	context.fogFlag.update();
        }

        /*
         * Model-View matrix has to reloaded when
         * - model matrix changed
         * - view matrix changed
         * - lighting has to be reloaded
         */
        boolean loadLightingSettings = (viewMatrixUpload.isChanged() || lightingChanged) && context.lightingFlag.isEnabled() && context.transform_mode == VTYPE_TRANSFORM_PIPELINE_TRANS_COORD;
        boolean modelViewMatrixChanged = modelMatrixUpload.isChanged() || viewMatrixUpload.isChanged() || loadLightingSettings;

        /*
         * Apply view matrix
         */
        if (modelViewMatrixChanged) {
            if (context.transform_mode == VTYPE_TRANSFORM_PIPELINE_TRANS_COORD) {
            	re.setViewMatrix(context.view_uploaded_matrix);
            } else {
            	re.setViewMatrix(null);
            }
            viewMatrixUpload.setChanged(false);
        }

        /*
         *  Setup lights on when view transformation is set up
         */
        if (loadLightingSettings || context.tex_map_mode == TMAP_TEXTURE_MAP_MODE_ENVIRONMENT_MAP) {
            for (int i = 0; i < NUM_LIGHTS; i++) {
                if (context.lightFlags[i].isEnabled() || (context.tex_map_mode == TMAP_TEXTURE_MAP_MODE_ENVIRONMENT_MAP && (context.tex_shade_u == i || context.tex_shade_v == i))) {
                	re.setLightPosition(i, context.light_pos[i]);
                	re.setLightDirection(i, context.light_dir[i]);

                    if (context.light_type[i] == LIGHT_SPOT) {
                    	re.setLightSpotExponent(i, context.spotLightExponent[i]);
                    	re.setLightSpotCutoff(i, context.spotLightCutoff[i]);
                    } else {
                        // uniform light distribution
                    	re.setLightSpotExponent(i, 0);
                    	re.setLightSpotCutoff(i, 180);
                    }

                    // Light kind:
                    //  LIGHT_DIFFUSE_SPECULAR: use ambient, diffuse and specular colors
                    //  all other light kinds: use ambient and diffuse colors (not specular)
                    if (context.light_kind[i] != LIGHT_AMBIENT_DIFFUSE) {
                    	re.setLightSpecularColor(i, context.lightSpecularColor[i]);
                    } else {
                    	re.setLightSpecularColor(i, blackColor);
                    }
                }
            }

            lightingChanged = false;
        }

        if (modelViewMatrixChanged) {
            // Apply model matrix
            if (context.transform_mode == VTYPE_TRANSFORM_PIPELINE_TRANS_COORD) {
            	re.setModelMatrix(context.model_uploaded_matrix);
            } else {
            	re.setModelMatrix(null);
            }
            modelMatrixUpload.setChanged(false);
            re.endModelViewMatrixUpdate();
        }

        /*
         * Apply texture transforms
         */
        if (textureMatrixUpload.isChanged()) {
            if (context.transform_mode != VTYPE_TRANSFORM_PIPELINE_TRANS_COORD) {
            	re.setTextureMapMode(TMAP_TEXTURE_MAP_MODE_TEXTURE_COORDIATES_UV, TMAP_TEXTURE_PROJECTION_MODE_TEXTURE_COORDINATES);
            	context.reTextureGenS.setEnabled(false);
            	context.reTextureGenT.setEnabled(false);

            	float[] textureMatrix = new float[] {
            			1.f / context.texture_width[0], 0, 0, 0,
                		0, 1.f / context.texture_height[0], 0, 0,
                		0, 0, 1, 0,
                		0, 0, 0, 1
                	};
            	if (textureFlipped) {
            		textureMatrix[5] = -textureMatrix[5];
            		textureMatrix[13] = textureFlipTranslateY;
            		if (isLogDebugEnabled) {
            			log.debug("Flipped 2D");
            		}
            	}
            	re.setTextureMatrix(textureMatrix);
            } else {
            	re.setTextureMapMode(context.tex_map_mode, context.tex_proj_map_mode);
                switch (context.tex_map_mode) {
                    case TMAP_TEXTURE_MAP_MODE_TEXTURE_COORDIATES_UV: {
                    	context.reTextureGenS.setEnabled(false);
                    	context.reTextureGenT.setEnabled(false);

                        float[] textureMatrix = new float[] {
                        		context.tex_scale_x, 0, 0, 0,
                        		0, context.tex_scale_y, 0, 0,
                        		0, 0, 1, 0,
                        		context.tex_translate_x, context.tex_translate_y, 0, 1
                        	};
                        if (textureFlipped) {
                    		textureMatrix[5] = -textureMatrix[5];
                    		if (isLogDebugEnabled) {
                    			log.debug("Flipped TMAP_TEXTURE_MAP_MODE_TEXTURE_COORDIATES_UV");
                    		}
                        }
                    	re.setTextureMatrix(textureMatrix);
                        break;
                    }

                    case TMAP_TEXTURE_MAP_MODE_TEXTURE_MATRIX: {
                    	context.reTextureGenS.setEnabled(false);
                    	context.reTextureGenT.setEnabled(false);
                    	float[] textureMatrix = context.texture_uploaded_matrix;
                    	if (textureFlipped) {
                    		// Map the (U,V) from ([0..1],[0..1]) to ([0..1],[1..0])
                    		float[] flippedTextureMatrix = new float[] {
                    			textureMatrix[0], textureMatrix[1], textureMatrix[2], textureMatrix[3],
                    			-textureMatrix[4], -textureMatrix[5], -textureMatrix[6], textureMatrix[7],
                    			textureMatrix[8], textureMatrix[9], textureMatrix[10], textureMatrix[11],
                    			textureMatrix[12] + textureMatrix[4], textureMatrix[13] + textureMatrix[5], textureMatrix[14] + textureMatrix[6], textureMatrix[15]
                    		};
                    		textureMatrix = flippedTextureMatrix;
                    		if (isLogDebugEnabled) {
                    			log.debug("Flipped TMAP_TEXTURE_MAP_MODE_TEXTURE_MATRIX");
                    		}
                    	}
                    	re.setTextureMatrix(textureMatrix);
                        break;
                    }

                    case TMAP_TEXTURE_MAP_MODE_ENVIRONMENT_MAP: {
                    	re.setTextureEnvironmentMapping(context.tex_shade_u, context.tex_shade_v);
                    	context.reTextureGenS.setEnabled(true);
                    	context.reTextureGenT.setEnabled(true);
                        for (int i = 0; i < 3; i++) {
                        	context.tex_envmap_matrix[i + 0] = context.light_pos[context.tex_shade_u][i];
                        	context.tex_envmap_matrix[i + 4] = context.light_pos[context.tex_shade_v][i];
                        }
                    	float[] textureMatrix = context.tex_envmap_matrix;
                    	if (textureFlipped) {
                    		// Map the (U,V) from ([0..1],[0..1]) to ([0..1],[1..0])
                    		float[] flippedTextureMatrix = new float[] {
                    			textureMatrix[0], textureMatrix[1], textureMatrix[2], textureMatrix[3],
                    			-textureMatrix[4], -textureMatrix[5], -textureMatrix[6], textureMatrix[7],
                    			textureMatrix[8], textureMatrix[9], textureMatrix[10], textureMatrix[11],
                    			textureMatrix[12] + textureMatrix[4], textureMatrix[13] + textureMatrix[5], textureMatrix[14] + textureMatrix[6], textureMatrix[15]
                    		};
                    		textureMatrix = flippedTextureMatrix;
                    		if (isLogDebugEnabled) {
                    			log.debug("Flipped TMAP_TEXTURE_MAP_MODE_ENVIRONMENT_MAP");
                    		}
                    	}
                    	re.setTextureMatrix(textureMatrix);
                        break;
                    }

                    default:
                        log("Unhandled texture matrix mode " + context.tex_map_mode);
                }
            }

            textureMatrixUpload.setChanged(false);
        }

        boolean useVertexColor = false;
        if (!context.lightingFlag.isEnabled() || context.transform_mode == VTYPE_TRANSFORM_PIPELINE_RAW_COORD) {
        	context.reColorMaterial.setEnabled(false);
            if (vinfo.color != 0) {
                useVertexColor = true;
            } else {
            	re.setVertexColor(context.mat_ambient);
            }
        } else if (vinfo.color != 0 && context.mat_flags != 0) {
            useVertexColor = true;
            if (materialChanged) {
            	boolean ambient = (context.mat_flags & 1) != 0;
            	boolean diffuse = (context.mat_flags & 2) != 0;
            	boolean specular = (context.mat_flags & 4) != 0;
                re.setColorMaterial(ambient, diffuse, specular);
                context.reColorMaterial.setEnabled(true);
                if (!ambient) {
                	re.setMaterialAmbientColor(context.mat_ambient);
                }
                if (!diffuse) {
                	re.setMaterialDiffuseColor(context.mat_diffuse);
                }
                if (!specular) {
                	re.setMaterialSpecularColor(context.mat_specular);
                }
                materialChanged = false;
            }
        	re.setVertexColor(context.mat_ambient);
        } else {
        	context.reColorMaterial.setEnabled(false);
            if (materialChanged) {
            	re.setColorMaterial(false, false, false);
            	re.setMaterialAmbientColor(context.mat_ambient);
            	re.setMaterialDiffuseColor(context.mat_diffuse);
            	re.setMaterialSpecularColor(context.mat_specular);
                materialChanged = false;
            }
        	re.setVertexColor(context.mat_ambient);
        }

        if (context.textureFlag.isEnabled()) {
        	re.setTextureWrapMode(context.tex_wrap_s, context.tex_wrap_t);

        	int validNumberMipmaps = getValidNumberMipmaps();
	        int mipmapBaseLevel = 0;
	        int mipmapMaxLevel = validNumberMipmaps;
	        if (context.tex_mipmap_mode == TBIAS_MODE_CONST) {
	            // TBIAS_MODE_CONST uses the tex_mipmap_bias_int level supplied by TBIAS.
	            mipmapBaseLevel = context.tex_mipmap_bias_int;
	            mipmapMaxLevel = context.tex_mipmap_bias_int;
	            if (isLogDebugEnabled) {
	                log.debug("TBIAS_MODE_CONST " + context.tex_mipmap_bias_int);
	            }
	        } else if (context.tex_mipmap_mode == TBIAS_MODE_AUTO) {
	        	// TODO implement TBIAS_MODE_AUTO. The following is not correct
	            // TBIAS_MODE_AUTO performs a comparison between the texture's weight and height at level 0.
	            // int maxValue = Math.max(context.texture_width[0], context.texture_height[0]);
	        	//
	            // if(maxValue <= 1) {
	            //     mipmapBaseLevel = 0;
	            // } else {
	            //     mipmapBaseLevel = (int) ((Math.log((Math.abs(maxValue) / Math.abs(context.zpos))) / Math.log(2)) + context.tex_mipmap_bias);
	            // }
	            // mipmapMaxLevel = mipmapBaseLevel;
	            // if (isLogDebugEnabled) {
	            //     log.debug("TBIAS_MODE_AUTO " + context.tex_mipmap_bias + ", param=" + maxValue);
	            // }
	        } else if (context.tex_mipmap_mode == TBIAS_MODE_SLOPE) {
	            // TBIAS_MODE_SLOPE uses the tslope_level level supplied by TSLOPE.
	            mipmapBaseLevel = (int) ((Math.log(Math.abs(context.tslope_level) / Math.abs(context.zpos)) / Math.log(2)) + context.tex_mipmap_bias);
	            mipmapMaxLevel = mipmapBaseLevel;
	            if (isLogDebugEnabled) {
	                log.debug("TBIAS_MODE_SLOPE " + context.tex_mipmap_bias + ", slope=" + context.tslope_level);
	            }
	        }

	        // Clamp to [0..validNumberMipmaps]
	        mipmapBaseLevel = Math.max(0, Math.min(mipmapBaseLevel, validNumberMipmaps));
	        // Clamp to [mipmapBaseLevel..validNumberMipmaps]
	        mipmapMaxLevel = Math.max(mipmapBaseLevel, Math.min(mipmapMaxLevel, validNumberMipmaps));
	        if (isLogDebugEnabled) {
	            log.debug(String.format("Texture Mipmap base=%d, max=%d, validNumberMipmaps=%d", mipmapBaseLevel, mipmapMaxLevel, validNumberMipmaps));
	        }
	        re.setTextureMipmapMinLevel(mipmapBaseLevel);
	        re.setTextureMipmapMaxLevel(mipmapMaxLevel);
        }

        return useVertexColor;
    }

    private void endRendering(boolean useVertexColor, boolean useTexture, int numberOfVertex) {
        Memory mem = Memory.getInstance();

        // VADDR/IADDR are updated after vertex rendering
        // (IADDR when indexed and VADDR when not).
        // Some games rely on this and don't reload VADDR/IADDR between 2 PRIM/BBOX calls.
        if (vinfo.index == 0) {
            vinfo.ptr_vertex = vinfo.getAddress(mem, numberOfVertex);
        } else {
            vinfo.ptr_index += numberOfVertex * vinfo.index;
        }
    }

    public static float[] getOrthoMatrix(float left, float right, float bottom, float top, float near, float far) {
    	float dx = right - left;
    	float dy = top - bottom;
    	float dz = far - near;
    	float[] orthoMatrix = {
        		2.f / dx, 0, 0, 0,
        		0, 2.f / dy, 0, 0,
        		0, 0, -2.f / dz, 0,
        		-(right + left) / dx, -(top + bottom) / dy, -(far + near) / dz, 1
        };

    	return orthoMatrix;
    }

    float spline_n(int i, int j, float u, int[] knot) {
    	if(j == 0) {
    		if(knot[i] <= u && u < knot[i + 1])
    			return 1;
    		return 0;
    	}
    	float res = 0;
    	if(knot[i + j] - knot[i] != 0)
    	    res += (u - knot[i]) / (knot[i + j] - knot[i]) * spline_n(i, j - 1, u, knot);
    	if(knot[i + j + 1] - knot[i + 1] != 0)
    		res += (knot[i + j + 1] - u) / (knot[i + j + 1] - knot[i + 1]) * spline_n(i + 1, j - 1, u, knot);
    	return res;
    }

    int[] spline_knot(int n, int type) {
    	int[] knot = new int[n + 5];
    	for(int i = 0; i < n - 1; i++) {
    		knot[i + 3] = i;
        }

    	if((type & 1) == 0) {
    		knot[0] = -3;
    		knot[1] = -2;
    		knot[2] = -1;
    	}
    	if((type & 2) == 0) {
    		knot[n + 2] = n - 1;
    		knot[n + 3] = n;
    		knot[n + 4] = n + 1;
    	} else {
    		knot[n + 2] = n - 2;
    		knot[n + 3] = n - 2;
    		knot[n + 4] = n - 2;
    	}

    	return knot;
    }

    private void drawSpline(int ucount, int vcount, int utype, int vtype) {
        if (ucount < 4 || vcount < 4) {
            log.warn("Unsupported spline parameters uc=" + ucount + " vc=" + vcount);
            return;
        }
        if (context.patch_div_s <= 0 || context.patch_div_t <= 0) {
            log.warn("Unsupported spline patches patch_div_s=" + context.patch_div_s + " patch_div_t=" + context.patch_div_t);
            return;
        }

        boolean useVertexColor = initRendering();
        boolean useTexture = vinfo.texture != 0 || context.textureFlag.isEnabled();
        boolean useNormal = context.lightingFlag.isEnabled();

        // Generate control points.
        VertexState[][] ctrlpoints = getControlPoints(ucount, vcount);

        // GE capture.
        if (State.captureGeNextFrame && !isVertexBufferEmbedded()) {
            log.info("Capture drawSpline");
            CaptureManager.captureRAM(vinfo.ptr_vertex, vinfo.vertexSize * ucount * vcount);
        }

        // Generate patch VertexState.
        VertexState[][] patch = new VertexState[context.patch_div_s + 1][context.patch_div_t + 1];

        // Calculate knot arrays.
        int n = ucount - 1;
        int m = vcount - 1;
        int[] knot_u = spline_knot(n, utype);
        int[] knot_v = spline_knot(m, vtype);

        // The spline grows to a limit defined by n - 2 for u and m - 2 for v.
        // This limit is open, so we need to get a very close approximation of it.
        float limit = 2.000001f;

        // Process spline vertexes with Cox-deBoor's algorithm.
        for(int j = 0; j <= context.patch_div_t; j++) {
        	float v = (float)j * (float)(m - limit) / (float)context.patch_div_t;

        	for(int i = 0; i <= context.patch_div_s; i++) {
        		float u = (float)i * (float)(n - limit) / (float)context.patch_div_s;

        		patch[i][j] = new VertexState();
        		VertexState p = patch[i][j];

        		for(int ii = 0; ii <= n; ii++) {
        			for(int jj = 0; jj <= m; jj++) {
        				float f = spline_n(ii, 3, u, knot_u) * spline_n(jj, 3, v, knot_v);
        				if(f != 0) {
        					pointMultAdd(p, ctrlpoints[ii][jj], f, useVertexColor, useTexture, useNormal);
        				}
        			}
        		}
        		if(useTexture && vinfo.texture == 0) {
        			p.t[0] = u;
        			p.t[1] = v;
        		}
        	}
        }

        drawCurvedSurface(patch, ucount, vcount, useVertexColor, useTexture, useNormal);
    }

	private void pointMultAdd(VertexState dest, VertexState src, float f, boolean useVertexColor, boolean useTexture, boolean useNormal) {
		dest.p[0] += f * src.p[0];
		dest.p[1] += f * src.p[1];
		dest.p[2] += f * src.p[2];
		if(useTexture) {
			dest.t[0] += f * src.t[0];
			dest.t[1] += f * src.t[1];
		}
		if(useVertexColor) {
			dest.c[0] += f * src.c[0];
			dest.c[1] += f * src.c[1];
			dest.c[2] += f * src.c[2];
			dest.c[3] += f * src.c[3];
		}
		if(useNormal) {
			dest.n[0] += f * src.n[0];
			dest.n[1] += f * src.n[1];
			dest.n[2] += f * src.n[2];
		}
	}

    private void drawBezier(int ucount, int vcount) {
        if ((ucount - 1) % 3 != 0 || (vcount - 1) % 3 != 0) {
            log.warn("Unsupported bezier parameters ucount=" + ucount + " vcount=" + vcount);
            return;
        }
        if (context.patch_div_s <= 0 || context.patch_div_t <= 0) {
            log.warn("Unsupported bezier patches patch_div_s=" + context.patch_div_s + " patch_div_t=" + context.patch_div_t);
            return;
        }

        boolean useVertexColor = initRendering();
        boolean useTexture = vinfo.texture != 0 || context.textureFlag.isEnabled();
        boolean useNormal = context.lightingFlag.isEnabled();

        VertexState[][] anchors = getControlPoints(ucount, vcount);

        // Don't capture the ram if the vertex list is embedded in the display list. TODO handle stall_addr == 0 better
        // TODO may need to move inside the loop if indices are used, or find the largest index so we can calculate the size of the vertex list
        if (State.captureGeNextFrame && !isVertexBufferEmbedded()) {
            log.info("Capture drawBezier");
            CaptureManager.captureRAM(vinfo.ptr_vertex, vinfo.vertexSize * ucount * vcount);
        }

        // Generate patch VertexState.
        VertexState[][] patch = new VertexState[context.patch_div_s + 1][context.patch_div_t + 1];

        // Number of patches in the U and V directions
        int upcount = ucount / 3;
        int vpcount = vcount / 3;

        float[][] ucoeff = new float[context.patch_div_s + 1][];

        for(int j = 0; j <= context.patch_div_t; j++) {
        	float vglobal = (float)j * vpcount / (float)context.patch_div_t;

        	int vpatch = (int)vglobal; // Patch number
        	float v = vglobal - vpatch;
        	if(j == context.patch_div_t) {
    			vpatch--;
    			v = 1.f;
    		}
        	float[] vcoeff = BernsteinCoeff(v);

        	for(int i = 0; i <= context.patch_div_s; i++) {
        		float uglobal = (float)i * upcount / (float)context.patch_div_s;
        		int upatch = (int)uglobal;
        		float u = uglobal - upatch;
        		if(i == context.patch_div_s) {
        			upatch--;
        			u = 1.f;
        		}
        		ucoeff[i] = BernsteinCoeff(u);

        		patch[i][j] = new VertexState();
        		VertexState p = patch[i][j];

        		for(int ii = 0; ii < 4; ++ii) {
        			for(int jj = 0; jj < 4; ++jj) {
        				pointMultAdd(p,
        						anchors[3 * upatch + ii][3 * vpatch + jj],
        						ucoeff[i][ii] * vcoeff[jj],
        						useVertexColor, useTexture, useNormal);
        			}
        		}

        		if(useTexture && vinfo.texture == 0) {
        			p.t[0] = uglobal;
        			p.t[1] = vglobal;
        		}
        	}
        }

        drawCurvedSurface(patch, ucount, vcount, useVertexColor, useTexture, useNormal);
    }

	private void drawCurvedSurface(VertexState[][] patch, int ucount, int vcount,
			boolean useVertexColor, boolean useTexture, boolean useNormal) {
		if (re.isVertexArrayAvailable()) {
			re.bindVertexArray(0);
		}
		// TODO: Compute the normals
		setDataPointers(3, useVertexColor, 4, useTexture, 2, useNormal, 0, true);

		int type = patch_prim_types[context.patch_prim];
		re.setVertexInfo(vinfo, false, useVertexColor, useTexture, type);

		ByteBuffer drawByteBuffer = bufferManager.getBuffer(bufferId);
		drawByteBuffer.clear();
		FloatBuffer drawFloatBuffer = drawByteBuffer.asFloatBuffer();
        for(int j = 0; j <= context.patch_div_t - 1; j++) {
        	drawFloatBuffer.clear();

        	for(int i = 0; i <= context.patch_div_s; i++) {
        		VertexState v1 = patch[i][j];
                VertexState v2 = patch[i][j + 1];

        		if(useTexture)     drawFloatBuffer.put(v1.t);
        		if(useVertexColor) drawFloatBuffer.put(v1.c);
        		if(useNormal)      drawFloatBuffer.put(v1.n);
        		drawFloatBuffer.put(v1.p);

        		if(useTexture)     drawFloatBuffer.put(v2.t);
        		if(useVertexColor) drawFloatBuffer.put(v2.c);
        		if(useNormal)      drawFloatBuffer.put(v2.n);
        		drawFloatBuffer.put(v2.p);
        	}

        	bufferManager.setBufferData(bufferId, drawFloatBuffer.position() * SIZEOF_FLOAT, drawByteBuffer.rewind(), IRenderingEngine.RE_STREAM_DRAW);
    		drawArraysStatistics.start();
            re.drawArrays(type, 0, (context.patch_div_s + 1) * 2);
        	drawArraysStatistics.end();
        }

        endRendering(useVertexColor, useTexture, ucount * vcount);
	}

	private VertexState[][] getControlPoints(int ucount, int vcount) {
		VertexState[][] controlPoints = new VertexState[ucount][vcount];

		Memory mem = Memory.getInstance();
        for (int u = 0; u < ucount; u++) {
            for (int v = 0; v < vcount; v++) {
                int addr = vinfo.getAddress(mem, v * ucount + u);
                VertexState vs = vinfo.readVertex(mem, addr);
                if (isLogDebugEnabled) {
                	log(String.format("control point #%d,%d p(%f,%f,%f) t(%f,%f), c(%f,%f,%f)",
                			u, v,
                			vs.p[0], vs.p[1], vs.p[2],
                			vs.t[0], vs.t[1],
                			vs.c[0], vs.c[1], vs.c[2]));
                }
                controlPoints[u][v] = vs;
            }
        }
        return controlPoints;
	}

    private float[] BernsteinCoeff(float u) {
        float uPow2 = u * u;
        float uPow3 = uPow2 * u;
        float u1 = 1 - u;
        float u1Pow2 = u1 * u1;
        float u1Pow3 = u1Pow2 * u1;
        return new float[] {u1Pow3, 3 * u * u1Pow2, 3 * uPow2 * u1, uPow3 };
    }

    private Buffer getTextureBuffer(int texaddr, int bytesPerPixel, int level) {
        Buffer final_buffer = null;

        if (!context.texture_swizzle) {
            // texture_width might be larger than texture_buffer_width
            int bufferlen = Math.max(context.texture_buffer_width[level], context.texture_width[level]) * context.texture_height[level] * bytesPerPixel;
            final_buffer = Memory.getInstance().getBuffer(texaddr, bufferlen);

            if (State.captureGeNextFrame) {
                log.info("Capture getTextureBuffer unswizzled");
                CaptureManager.captureRAM(texaddr, bufferlen);
            }
        } else {
            final_buffer = unswizzleTextureFromMemory(texaddr, bytesPerPixel, level);
        }

        return final_buffer;
    }

    public final static String getPsmName(final int psm) {
        return (psm >= 0 && psm < psm_names.length)
                ? psm_names[psm % psm_names.length]
                : "PSM_UNKNOWN" + psm;
    }

    public final static String getLOpName(final int ops) {
        return (ops >= 0 && ops < logical_ops_names.length)
                ? logical_ops_names[ops % logical_ops_names.length]
                : "UNKNOWN_LOP" + ops;
    }

    private int getCompressedTextureSize(int level, int compressionRatio) {
        return getCompressedTextureSize(context.texture_width[level], context.texture_height[level], compressionRatio);
    }

    public static int getCompressedTextureSize(int width, int height, int compressionRatio) {
        int compressedTextureWidth = ((width + 3) / 4) * 4;
        int compressedTextureHeight = ((height + 3) / 4) * 4;
        int compressedTextureSize = compressedTextureWidth * compressedTextureHeight * 4 / compressionRatio;

        return compressedTextureSize;
    }

    private void updateGeBuf() {
        if (geBufChanged) {
            display.hleDisplaySetGeBuf(context.fbp, context.fbw, context.psm, somethingDisplayed, forceLoadGEToScreen);
            forceLoadGEToScreen = false;
            geBufChanged = false;

            textureChanged = true;
            maxSpriteHeight = 0;
            maxSpriteWidth = 0;
            projectionMatrixUpload.setChanged(true);
            modelMatrixUpload.setChanged(true);
            viewMatrixUpload.setChanged(true);
            textureMatrixUpload.setChanged(true);
            viewportChanged = true;
            depthChanged = true;
            materialChanged = true;
        }
    }
    // For capture/replay

    public int getFBP() {
        return context.fbp;
    }

    public int getFBW() {
        return context.fbw;
    }

    public int getZBP() {
        return context.zbp;
    }

    public int getZBW() {
        return context.zbw;
    }

    public int getPSM() {
        return context.psm;
    }

    private boolean isVertexBufferEmbedded() {
        // stall_addr may be 0
        return (vinfo.ptr_vertex >= currentList.list_addr && vinfo.ptr_vertex < currentList.getStallAddr());
    }

    public boolean isVRAM(int addr) {
        addr &= Memory.addressMask;

        return addr >= MemoryMap.START_VRAM && addr <= MemoryMap.END_VRAM;
    }

    private void hlePerformAction(IAction action, Semaphore sync) {
        hleAction = action;

        while (true) {
            try {
                sync.acquire();
                break;
            } catch (InterruptedException e) {
                // Retry again..
            }
        }
    }

    public void hleSaveContext(int addr) {
        // If we are rendering, we have to wait for a consistent state
        // before saving the context: let the display thread perform
        // the save when appropriate.
        if (hasDrawLists() || currentList != null) {
            Semaphore sync = new Semaphore(0);
            hlePerformAction(new SaveContextAction(addr, sync), sync);
        } else {
            saveContext(addr);
        }
    }

    public void hleRestoreContext(int addr) {
        // If we are rendering, we have to wait for a consistent state
        // before restoring the context: let the display thread perform
        // the restore when appropriate.
        if (hasDrawLists() || currentList != null) {
            Semaphore sync = new Semaphore(0);
            hlePerformAction(new RestoreContextAction(addr, sync), sync);
        } else {
            restoreContext(addr);
        }
    }

    private void saveContext(int addr) {
    	context.write(Memory.getInstance(), addr);
    }

    private void restoreContext(int addr) {
    	context.read(Memory.getInstance(), addr);
    	context.setDirty();

        projectionMatrixUpload.setChanged(true);
        modelMatrixUpload.setChanged(true);
        viewMatrixUpload.setChanged(true);
        textureMatrixUpload.setChanged(true);
        lightingChanged = true;
        textureChanged = true;
        geBufChanged = true;
        viewportChanged = true;
        depthChanged = true;
        materialChanged = true;
    }

    public boolean isUsingTRXKICK() {
        return usingTRXKICK;
    }

    public int getMaxSpriteHeight() {
        return maxSpriteHeight;
    }

    public int getMaxSpriteWidth() {
        return maxSpriteWidth;
    }

    private void setUseVertexCache(boolean useVertexCache) {
        // VertexCache is relying on VBO
    	if (bufferManager != null && !bufferManager.useVBO()) {
    		useVertexCache = false;
    	}

        this.useVertexCache = useVertexCache;
        if (useVertexCache) {
        	if (useAsyncVertexCache) {
        		AsyncVertexCache.getInstance();
        		if (useOptimisticVertexCache) {
        			log.info("Using Optimistic Async Vertex Cache");
        		} else {
        			log.info("Using Async Vertex Cache");
        		}
        	} else {
        		VertexCache.getInstance();
        		if (useOptimisticVertexCache) {
        			log.info("Using Optimistic Vertex Cache");
        		} else {
        			log.info("Using Vertex Cache");
        		}
        	}
        }
    }

    public boolean useAsyncVertexCache() {
    	return useVertexCache && useAsyncVertexCache;
    }

    public int getBase() {
        return context.base;
    }

    public void setBase(int base) {
    	context.base = base;
    }

    public int getBaseOffset() {
        return context.baseOffset;
    }

    public void setBaseOffset(int baseOffset) {
    	context.baseOffset = baseOffset;
    }

    public void addVideoTexture(int startAddress, int endAddress) {
		// Synchronize the access to videoTextures as it can be accessed
		// from a parallel threads (async display and PSP thread)
    	synchronized (videoTextures) {
        	for (AddressRange addressRange : videoTextures) {
        		if (addressRange.equals(startAddress, endAddress)) {
        			return;
        		}
        	}

        	AddressRange addressRange = new AddressRange(startAddress, endAddress);
        	videoTextures.add(addressRange);
		}
    }

    public void resetVideoTextures() {
		// Synchronize the access to videoTextures as it can be accessed
		// from a parallel threads (async display and PSP thread)
    	synchronized (videoTextures) {
        	videoTextures.clear();
		}
    }

    protected void matrixMult(float[] result, float[] m1, float[] m2) {
    	// If the result has to be stored into one of the input matrix,
    	// store the result in a temp array first.
    	float[] origResult = null;
    	if (result == m1 || result == m2) {
    		origResult = result;
    		result = new float[4 * 4];
    	}

    	for (int i = 0; i < 4; i++) {
    		for (int j = 0; j < 4; j++) {
    			float s = 0;
    			for (int k = 0; k < 4; k++) {
    				s += m1[k * 4 + j] * m2[i * 4 + k];
    			}
    			result[i * 4 + j] = s;
    		}
    	}

    	if (origResult != null) {
    		System.arraycopy(result, 0, origResult, 0, result.length);
    	}
    }

    protected void vectorMult(float[] result, float[] m, float[] v) {
    	for (int i = 0; i < result.length; i++) {
    		float s = 0;
    		for (int j = 0; j < v.length; j++) {
    			s += v[j] * m[j * 4 + i];
    		}
    		result[i] = s;
    	}
    }

	public boolean isUseTextureAnisotropicFilter() {
		return useTextureAnisotropicFilter;
	}

	public void setUseTextureAnisotropicFilter(boolean useTextureAnisotropicFilter) {
		this.useTextureAnisotropicFilter = useTextureAnisotropicFilter;
	}

    private class SaveContextAction implements IAction {
        private int addr;
        private Semaphore sync;

        public SaveContextAction(int addr, Semaphore sync) {
            this.addr = addr;
            this.sync = sync;
        }

        @Override
        public void execute() {
            saveContext(addr);
            sync.release();
        }
    }

    private class RestoreContextAction implements IAction {
        private int addr;
        private Semaphore sync;

        public RestoreContextAction(int addr, Semaphore sync) {
            this.addr = addr;
            this.sync = sync;
        }

        @Override
        public void execute() {
            restoreContext(addr);
            context.update();
            sync.release();
        }
    }

    private static class AddressRange {
    	private int start;
    	private int end;

    	public AddressRange(int start, int end) {
    		this.start = start & Memory.addressMask;
    		this.end = end & Memory.addressMask;
    	}

    	public boolean contains(int address) {
    		address &= Memory.addressMask;

    		return address >= start && address < end;
    	}

    	public boolean equals(int start, int end) {
    		start &= Memory.addressMask;
    		end &= Memory.addressMask;

    		return start == this.start && end == this.end;
    	}
    }
}
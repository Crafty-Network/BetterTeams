package com.booksaw.betterTeams.extension;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.exceptions.LoadingException;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static com.booksaw.betterTeams.extension.ExtensionTestUtil.createFakeJar;
import static com.booksaw.betterTeams.extension.ExtensionTestUtil.createYml;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ExtensionLoader Tests")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExtensionLoaderTest {

	@TempDir
	Path tempDir;

	private ExtensionLoader loader;
	@Mock
	private Main mockPlugin;
	@Mock
	private Logger mockLogger;
	@Mock
	private ExtensionManager mockExtManager;
	@Mock
	private PluginManager mockPluginManager;

	@BeforeEach
	void setUp() {
		Server mockServer = mock(Server.class);

		when(mockPlugin.getLogger()).thenReturn(mockLogger);
		when(mockPlugin.getServer()).thenReturn(mockServer);
		when(mockServer.getPluginManager()).thenReturn(mockPluginManager);

		Main.plugin = mockPlugin;

		loader = new ExtensionLoader(mockPlugin);
	}

	@AfterEach
	void tearDown() {
		Main.plugin = null;
	}

	private ExtensionInfo createValidInfo(String name) throws IOException {
		String yml = createYml(name);
		File jar = createFakeJar(name + ".jar", yml, TestExtensionImpl.class, tempDir.toFile());
		return ExtensionInfo.fromYaml(jar);
	}

	@Nested
	@DisplayName("load() Tests")
	class LoadTests {

		@Test
		@DisplayName("Should successfully load a valid extension")
		void testLoadSuccess() throws IOException, LoadingException {
			ExtensionInfo info = createValidInfo("ValidExt");
			ExtensionWrapper wrapper = loader.load(info);

			assertNotNull(wrapper);
			assertNotNull(wrapper.getInstance());
			assertNotNull(wrapper.getClassLoader());
			assertEquals(info, wrapper.getInfo());
			assertFalse(wrapper.isEnabled());

			assertInstanceOf(TestExtensionImpl.class, wrapper.getInstance());
			TestExtensionImpl impl = (TestExtensionImpl) wrapper.getInstance();
			assertTrue(impl.onLoadCalled);
			assertFalse(impl.onEnableCalled);
			verify(mockLogger).info("Loaded extension: ValidExt");
		}

		@Test
		@DisplayName("Should throw LoadingException if JAR file is missing")
		void testLoadNoJar() throws IOException {
			ExtensionInfo mockInfo = mock(ExtensionInfo.class);

			when(mockInfo.getName()).thenReturn("NoJarExt");
			when(mockInfo.getJarFile()).thenReturn(null);

			LoadingException e = assertThrows(LoadingException.class, () -> {
				loader.load(mockInfo);
			});
			assertEquals("No JAR file associated with extension 'NoJarExt'", e.getMessage());;
		}

		@Test
		@DisplayName("Should throw LoadingException if main class does not extend BetterTeamsExtension")
		void testLoadWrongMainClass() throws IOException {
			String yml = createYml("WrongClass") + "\nmain: com.booksaw.betterTeams.extension.ExtensionLoaderTest";
			File jar = createFakeJar("wrongclass.jar", yml, ExtensionLoaderTest.class, tempDir.toFile());
			ExtensionInfo info = ExtensionInfo.fromYaml(jar);

			LoadingException e = assertThrows(LoadingException.class, () -> {
				loader.load(info);
			});

			assertTrue(e.getMessage().startsWith("Failed to initialize extension 'WrongClass'"));
			assertInstanceOf(ClassCastException.class, e.getCause());
		}
	}

	@Nested
	@DisplayName("enable() Tests")
	class EnableTests {

		private ExtensionWrapper validWrapper;
		private TestExtensionImpl validImpl;

		@BeforeEach
		void setUp() throws IOException, LoadingException {
			ExtensionInfo info = createValidInfo("ValidExt");
			validWrapper = loader.load(info); 
			validImpl = (TestExtensionImpl) validWrapper.getInstance();
		}

		@Test
		@DisplayName("Should successfully enable a loaded extension")
		void testEnableSuccess() throws LoadingException {

			loader.enable(validWrapper);

			assertTrue(validWrapper.isEnabled());
			assertTrue(validImpl.onEnableCalled);

			verify(mockLogger).info("Enabled extension: ValidExt");
		}

		@Test
		@DisplayName("Should not enable if already enabled")
		void testEnableAlreadyEnabled() throws LoadingException {
			validWrapper.setEnabled(true);
			validImpl.onEnableCalled = false; 

			ExtensionWrapper result = loader.enable(validWrapper);

			assertSame(validWrapper, result);
			assertFalse(validImpl.onEnableCalled);
		}

		@Test
		@DisplayName("Should throw LoadingException if a plugin dependency is missing")
		void testEnableFailsMissingPluginDep() throws IOException {
			
			String yml = createYml("DepExt") + "\nplugin-depend: [Vault]";
			File jar = createFakeJar("depext.jar", yml, TestExtensionImpl.class, tempDir.toFile());
			ExtensionInfo info = ExtensionInfo.fromYaml(jar);
			ExtensionWrapper depWrapper = assertDoesNotThrow(() -> loader.load(info));

			when(mockPluginManager.getPlugin("Vault")).thenReturn(null);

			LoadingException e = assertThrows(LoadingException.class, () -> {
				loader.enable(depWrapper);
			});

			assertEquals("Cannot enable 'DepExt': Missing Bukkit plugin dependencies", e.getMessage());
			assertNotNull(depWrapper);
			assertFalse(depWrapper.isEnabled());
		}

		@DisplayName("Should throw LoadingException if an extension dependency is missing")
		void testEnableFailsMissingExtensionDep() throws IOException {
			
			String yml = createYml("DepExt") + "\ndepend: [OtherExt]";
			File jar = createFakeJar("depext.jar", yml, TestExtensionImpl.class, tempDir.toFile());
			ExtensionInfo info = ExtensionInfo.fromYaml(jar);
			ExtensionWrapper depWrapper = assertDoesNotThrow(() -> loader.load(info));

			when(mockExtManager.isEnabled("OtherExt")).thenReturn(false);

			LoadingException e = assertThrows(LoadingException.class, () -> {
				loader.enable(depWrapper);
			});

			assertEquals("Cannot enable 'DepExt': Missing dependencies", e.getMessage());
			assertNotNull(depWrapper);
			assertFalse(depWrapper.isEnabled());
		}
	}

	@Nested
	@DisplayName("disable() Tests")
	class DisableTests {
		private ExtensionWrapper validWrapper;
		private TestExtensionImpl validImpl;

		@BeforeEach
		void setUp() throws IOException, LoadingException {
			ExtensionInfo info = createValidInfo("ValidExt");
			validWrapper = loader.load(info);
			validImpl = (TestExtensionImpl) validWrapper.getInstance();

			validWrapper.setEnabled(true);
			validImpl.onLoadCalled = false; 
		}

		@Test
		@DisplayName("Should successfully disable an enabled extension")
		void testDisableSuccess() {
			
			assertTrue(validWrapper.isEnabled());

			loader.disable(validWrapper);

			assertFalse(validWrapper.isEnabled());
			assertTrue(validImpl.onDisableCalled);

			verify(mockLogger).info("Disabled extension: ValidExt");
		}

		@Test
		@DisplayName("Should do nothing if already disabled")
		void testDisableAlreadyDisabled() {
			
			validWrapper.setEnabled(false);

			loader.disable(validWrapper);

			assertFalse(validImpl.onDisableCalled);
			
			verify(mockLogger, never()).info("Disabled extension: ValidExt");
		}
	}

	@Nested
	@DisplayName("unload() Tests")
	class UnloadTests {

		@Test
		@DisplayName("Should disable and unload a loaded extension")
		void testUnloadSuccess() throws IOException, LoadingException {
			
			ExtensionInfo info = createValidInfo("ValidExt");
			ExtensionWrapper wrapper = loader.load(info);
			TestExtensionImpl impl = (TestExtensionImpl) wrapper.getInstance();
			loader.enable(wrapper);
			assertTrue(wrapper.isEnabled());

			loader.unload(wrapper);

			assertFalse(wrapper.isEnabled());
			assertTrue(impl.onDisableCalled);

			ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
			verify(mockLogger, times(4)).info(captor.capture()); 

			List<String> logs = captor.getAllValues();
			assertEquals("Loaded extension: ValidExt", logs.get(0));
			assertEquals("Enabled extension: ValidExt", logs.get(1));
			assertEquals("Disabled extension: ValidExt", logs.get(2));
			assertEquals("Unloaded extension: ValidExt", logs.get(3));

			URLClassLoader cl = wrapper.getClassLoader();
			assertNotNull(cl);
		}
	}
}
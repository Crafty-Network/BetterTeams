package com.booksaw.betterTeams.extension;

import com.booksaw.betterTeams.Main;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static com.booksaw.betterTeams.extension.ExtensionTestUtil.createFakeJar;
import static com.booksaw.betterTeams.extension.ExtensionTestUtil.createYml;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ExtensionClassLoader Global Access Tests")
class ExtensionClassLoaderTest {

	@TempDir
	Path tempDir;

	@Mock
	private Main mockPlugin;
	@Mock
	private ExtensionManager mockManager;
	@Mock
	private ExtensionStore mockStore;

	private ClassLoader isolatingParent;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		when(mockPlugin.getExtensionManager()).thenReturn(mockManager);
		when(mockManager.getStore()).thenReturn(mockStore);

		isolatingParent = new ClassLoader(getClass().getClassLoader()) {
			@Override
			protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
				
				if (name.equals(UniqueClassInExtA.class.getName()) ||
						name.equals(TestExtensionImpl.class.getName())) {
					throw new ClassNotFoundException(name);
				}
				return super.loadClass(name, resolve);
			}
		};
	}

	public static class UniqueClassInExtA {
		public static String greet() { return "Hello from A"; }
	}

	@Test
	@DisplayName("Should load a class from another extension (Global Access)")
	void testLoadClassFromOtherExtension() throws Exception {
		
		File jarA = createFakeJar("ExtA.jar", createYml("ExtA"), tempDir.toFile(),
				TestExtensionImpl.class, UniqueClassInExtA.class);

		ExtensionInfo infoA = ExtensionInfo.fromYaml(jarA);

		try (ExtensionClassLoader loaderA = new ExtensionClassLoader(jarA, isolatingParent, infoA, mockManager)) {

			ExtensionWrapper wrapperA = mock(ExtensionWrapper.class);
			when(wrapperA.getClassLoader()).thenReturn(loaderA);
			when(wrapperA.getInfo()).thenReturn(infoA);

			when(mockStore.getAll()).thenReturn(List.of(wrapperA));

			File jarB = createFakeJar("ExtB.jar", createYml("ExtB"), tempDir.toFile(), TestExtensionImpl.class);
			ExtensionInfo infoB = ExtensionInfo.fromYaml(jarB);

			try (ExtensionClassLoader loaderB = new ExtensionClassLoader(jarB, isolatingParent, infoB, mockManager)) {

				Class<?> loadedClass = assertDoesNotThrow(() -> {
					return loaderB.loadClass(UniqueClassInExtA.class.getName());
				});

				assertEquals(UniqueClassInExtA.class.getName(), loadedClass.getName());
				
				assertEquals(loaderA, loadedClass.getClassLoader());
			}
		}
	}

	@Test
	@DisplayName("Should prefer its own class over others")
	void testSelfPriority() throws Exception {
		
		File jarA = createFakeJar("ExtA.jar", createYml("ExtA"), tempDir.toFile(), TestExtensionImpl.class);
		ExtensionInfo infoA = ExtensionInfo.fromYaml(jarA);

		try (ExtensionClassLoader loaderA = new ExtensionClassLoader(jarA, isolatingParent, infoA, mockManager)) {
			ExtensionWrapper wrapperA = mock(ExtensionWrapper.class);
			when(wrapperA.getClassLoader()).thenReturn(loaderA);
			when(wrapperA.getInfo()).thenReturn(infoA);
			when(mockStore.getAll()).thenReturn(List.of(wrapperA));

			File jarB = createFakeJar("ExtB.jar", createYml("ExtB"), tempDir.toFile(), TestExtensionImpl.class);
			ExtensionInfo infoB = ExtensionInfo.fromYaml(jarB);

			try (ExtensionClassLoader loaderB = new ExtensionClassLoader(jarB, isolatingParent, infoB, mockManager)) {

				Class<?> loadedClass = loaderB.loadClass(TestExtensionImpl.class.getName());

				assertEquals(loaderB, loadedClass.getClassLoader());
				assertNotEquals(loaderA, loadedClass.getClassLoader());
			}
		}
	}

	@Test
	@DisplayName("Should fail if class does not exist anywhere")
	void testClassNotFound() throws Exception {
		File jarB = createFakeJar("ExtB.jar", createYml("ExtB"), tempDir.toFile(), TestExtensionImpl.class);
		ExtensionInfo infoB = ExtensionInfo.fromYaml(jarB);

		try (ExtensionClassLoader loaderB = new ExtensionClassLoader(jarB, isolatingParent, infoB, mockManager)) {
			when(mockStore.getAll()).thenReturn(List.of());

			assertThrows(ClassNotFoundException.class, () -> {
				loaderB.loadClass("com.booksaw.nonexistent.Clazz");
			});
		}
	}
}
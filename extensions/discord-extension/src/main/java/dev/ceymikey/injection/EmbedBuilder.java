
package dev.ceymikey.injection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;

@Getter
public class EmbedBuilder {

	private final String url;
	private final String title;
	private final String description;
	private final int color;
	private final List<Field> fields;
	private final String thumbnailUrl;
	private final String footerText;

	@Contract(pure = true)
	private EmbedBuilder(Construct construct) {
		this.url = construct.url;
		this.title = construct.title;
		this.description = construct.description;
		this.color = construct.color;
		this.fields = construct.fields;
		this.thumbnailUrl = construct.thumbnailUrl;
		this.footerText = construct.footerText;
	}

	public static class Construct {
		private final List<Field> fields = new ArrayList<>();
		private String url;
		private String title;
		private String description;
		private int color;
		private String thumbnailUrl;
		private String footerText;

		public Construct setThumbnailUrl(String thumbnailUrl) {
			this.thumbnailUrl = thumbnailUrl;
			return this;
		}

		public Construct setUrl(String url) {
			this.url = url;
			return this;
		}

		public Construct setTitle(String title) {
			this.title = title;
			return this;
		}

		public Construct setDescription(String description) {
			this.description = description;
			return this;
		}

		public Construct setColor(int color) {
			this.color = color;
			return this;
		}

		public Construct addField(String name, String value) {
			this.fields.add(new Field(name, value));
			return this;
		}

		public Construct setFooter(String footerText) {
			this.footerText = footerText;
			return this;
		}

		public EmbedBuilder build() {
			return new EmbedBuilder(this);
		}
	}

	@AllArgsConstructor
	public static class Field {
		public String name;
		public String value;
	}
}

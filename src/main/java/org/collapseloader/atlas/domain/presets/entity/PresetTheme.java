package org.collapseloader.atlas.domain.presets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class PresetTheme {
    @Column(name = "custom_css", columnDefinition = "TEXT")
    private String customCss;

    @Column(name = "enable_custom_css")
    private boolean enableCustomCss;

    @Column(name = "base_100")
    private String base100;

    @Column(name = "base_200")
    private String base200;

    @Column(name = "base_300")
    private String base300;

    @Column(name = "base_content")
    private String baseContent;

    @Column(name = "primary_color")
    private String primary;
    @Column(name = "primary_content")
    private String primaryContent;

    private String secondary;
    @Column(name = "secondary_content")
    private String secondaryContent;

    private String accent;
    @Column(name = "accent_content")
    private String accentContent;

    private String neutral;
    @Column(name = "neutral_content")
    private String neutralContent;

    private String info;
    @Column(name = "info_content")
    private String infoContent;

    private String success;
    @Column(name = "success_content")
    private String successContent;

    private String warning;
    @Column(name = "warning_content")
    private String warningContent;

    private String error;
    @Column(name = "error_content")
    private String errorContent;

    @Column(name = "background_image", columnDefinition = "TEXT")
    private String backgroundImage;

    @Column(name = "background_blur")
    private Double backgroundBlur;

    @Column(name = "background_opacity")
    private Double backgroundOpacity;
}

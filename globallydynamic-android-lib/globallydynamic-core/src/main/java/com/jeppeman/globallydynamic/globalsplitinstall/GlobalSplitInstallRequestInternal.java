package com.jeppeman.globallydynamic.globalsplitinstall;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * A request specifying what split APK:s to install
 */
class GlobalSplitInstallRequestInternal {
    private final List<String> moduleNames;
    private final List<Locale> languages;
    private final boolean includeMissing;

    private GlobalSplitInstallRequestInternal() {
        throw new IllegalStateException("Not allowed");
    }

    private GlobalSplitInstallRequestInternal(
            List<String> moduleNames,
            List<Locale> languages,
            boolean includeMissing) {
        this.moduleNames = moduleNames;
        this.languages = languages;
        this.includeMissing = includeMissing;
    }

    public List<String> getModuleNames() {
        return moduleNames;
    }

    public List<Locale> getLanguages() {
        return languages;
    }

    public boolean shouldIncludeMissingSplits() {
        return includeMissing;
    }

    /**
     * Creates a new {@link Builder} for a request
     *
     * @return a new {@link Builder}
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    public static GlobalSplitInstallRequestInternal fromExternal(GlobalSplitInstallRequest request) {
        GlobalSplitInstallRequestInternal.Builder builder =
                GlobalSplitInstallRequestInternal.newBuilder();

        for (String module : request.getModuleNames()) {
            builder.addModule(module);
        }

        for (Locale language : request.getLanguages()) {
            builder.addLanguage(language);
        }

        builder.shouldIncludeMissingSplits(false);

        return builder.build();
    }

    /**
     * Builder for the request
     */
    public static class Builder {
        private List<String> moduleNames = new LinkedList<String>();
        private List<Locale> languages = new LinkedList<Locale>();
        private boolean includeMissing;

        Builder() {

        }

        /**
         * Adds a module to the request, this must be a valid dynamic feature module name
         *
         * @param moduleName the name of a dynamic feature module
         * @return itself
         */
        public Builder addModule(String moduleName) {
            this.moduleNames.add(moduleName);
            return this;
        }

        /**
         * Adds a language to the request
         *
         * @param locale the language to add to the installation
         * @return itself
         */
        public Builder addLanguage(Locale locale) {
            this.languages.add(locale);
            return this;
        }

        /**
         * Whether or not to include missing splits
         *
         * @param includeMissing whether or not to include missing splits
         * @return itself
         */
        public Builder shouldIncludeMissingSplits(boolean includeMissing) {
            this.includeMissing = includeMissing;
            return this;
        }

        /**
         * Builds a new {@link GlobalSplitInstallRequestInternal} with the modules and languages
         * added to this builder.
         *
         * @return a new {@link GlobalSplitInstallRequestInternal}
         */
        public GlobalSplitInstallRequestInternal build() {
            return new GlobalSplitInstallRequestInternal(moduleNames, languages, includeMissing);
        }
    }
}

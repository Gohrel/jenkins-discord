package nz.co.jammehcow.jenkinsdiscord;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.model.JenkinsLocationConfiguration;
import nz.co.jammehcow.jenkinsdiscord.exception.WebhookException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Author: jammehcow.
 * Date: 22/04/17.
 */

public class WebhookPublisher extends Notifier {
    private final String webhookURL;
    private static final String NAME = "Discord Webhook";
    private static final String VERSION = "1.0.0";

    @DataBoundConstructor
    public WebhookPublisher(String webhookURL) { this.webhookURL = webhookURL; }

    public String getWebhookURL() { return this.webhookURL; }

    @Override
    public boolean needsToRunAfterFinalized() { return true; }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        JenkinsLocationConfiguration globalConfig = new JenkinsLocationConfiguration();

        if (this.webhookURL.isEmpty()) {
            listener.getLogger().println("The Discord webhook is not set!");
            return true;
        } else if (globalConfig.getUrl().isEmpty() || globalConfig.getUrl().contains("http://localhost")) {
            listener.getLogger().println("Your Jenkins URL is not set or is localhost!");
            return true;
        }

        StringBuilder changesList = new StringBuilder();

        for (Object o : build.getChangeSet().getItems()) changesList.append(" - *").append(o.toString()).append("*\n");

        StringBuilder artifacts = new StringBuilder();

        for (Object a : build.getArtifacts()) {
            Run.Artifact artifact = (Run.Artifact) a;
            artifacts.append(" - ").append(globalConfig.getUrl()).append(artifact.getHref()).append("\n");
        }

        DiscordWebhook wh = new DiscordWebhook(this.webhookURL);
        wh.setTitle(build.getProject().getDisplayName() + " " + build.getId());
        wh.setDescription("**Build:**  #" + build.getId() +
                "\n**Status:**  " + ((build.getBuildStatusSummary().message.equals("stable")) ? "Success" : "Failure") +
                ((changesList.length() != 0) ? "\n**Changes:**\n" + changesList.toString() : "") +
                ((artifacts.length() != 0) ? "\n**Artifacts:**\n" + artifacts.toString() : ""));
        wh.setStatus(build.getBuildStatusSummary().message.equals("stable"));
        wh.setURL(globalConfig.getUrl() + build.getUrl());
        wh.setFooter("Jenkins v" + build.getHudsonVersion() + ", " + getDescriptor().getDisplayName() + " v" + getDescriptor().getVersion());

        try { wh.send(); }
        catch (WebhookException e) { e.printStackTrace(); }

        return true;
    }


    public BuildStepMonitor getRequiredMonitorService() { return BuildStepMonitor.NONE; }


    @Override
    public DescriptorImpl getDescriptor() { return (DescriptorImpl) super.getDescriptor(); }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {


        public boolean isApplicable(Class<? extends AbstractProject> aClass) { return true; }

        public FormValidation doCheckWebhookURL(@QueryParameter String value) {
            // TODO: regex tester.
            if (!value.startsWith("https://discordapp.com/api/webhooks/"))
                return FormValidation.error("Please enter a valid Discord webhook URL.");
            return FormValidation.ok();
        }

        public String getDisplayName() { return NAME; }

        public String getVersion() { return VERSION; }
    }
}
package me.blvckbytes.quick_shop_search.command;

import me.blvckbytes.bbconfigmapper.ScalarType;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.PluginPermission;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class AboutSubCommand extends SubCommand {

  private final Plugin plugin;
  private final ConfigKeeper<MainSection> config;

  public AboutSubCommand(Plugin plugin, ConfigKeeper<MainSection> config) {
    super("about", PluginPermission.SUB_COMMAND_ABOUT.node);

    this.config = config;
    this.plugin = plugin;
  }

  @Override
  public ExitCode onCommand(CommandSender sender, String[] args) {
    if (args.length != 0)
      return ExitCode.MALFORMED_USAGE;

    BukkitEvaluable message;

    if ((message = config.rootSection.playerMessages.commandAboutScreenHeader) != null)
      message.sendMessage(sender, config.rootSection.builtBaseEnvironment);

    var pluginDescription = this.plugin.getDescription();

    config.rootSection.playerMessages.commandAboutScreen.sendMessage(
      sender,
      config.rootSection.getBaseEnvironment()
        .withStaticVariable("version", pluginDescription.getVersion())
        .withStaticVariable("authors", pluginDescription.getAuthors())
        .build()
    );

    if ((message = config.rootSection.playerMessages.commandAboutScreenFooter) != null)
      message.sendMessage(sender, config.rootSection.builtBaseEnvironment);

    return ExitCode.SUCCESS;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, String[] args) {
    return List.of();
  }

  @Override
  public String getUsage(CommandSender sender) {
    return config.rootSection.playerMessages.commandAboutUsage.asScalar(
      ScalarType.STRING, config.rootSection.getBaseEnvironment()
        .withStaticVariable("sub_label", label)
        .build()
    );
  }

  @Override
  public String getDescription(CommandSender sender) {
    return config.rootSection.playerMessages.commandAboutDescription.asScalar(
      ScalarType.STRING, config.rootSection.getBaseEnvironment()
        .withStaticVariable("sub_label", label)
        .build()
    );
  }
}

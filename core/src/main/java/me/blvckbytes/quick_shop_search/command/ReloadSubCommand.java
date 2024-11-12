package me.blvckbytes.quick_shop_search.command;

import me.blvckbytes.bbconfigmapper.ScalarType;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.quick_shop_search.PluginPermission;
import me.blvckbytes.quick_shop_search.config.MainSection;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReloadSubCommand extends SubCommand {

  private final Logger logger;
  private final ConfigKeeper<MainSection> config;

  public ReloadSubCommand(Logger logger, ConfigKeeper<MainSection> config) {
    super(SubCommandLabel.RELOAD, PluginPermission.SUB_COMMAND_RELOAD.node);

    this.logger = logger;
    this.config = config;
  }

  @Override
  public ExitCode onCommand(CommandSender sender, String[] args) {
    if (args.length != 0)
      return ExitCode.MALFORMED_USAGE;

    try {
      this.config.reload();

      config.rootSection.playerMessages.pluginReloadedSuccess.sendMessage(
        sender, config.rootSection.builtBaseEnvironment
      );
    } catch (Exception e) {
      logger.log(Level.SEVERE, "An error occurred while trying to reload the config", e);

      config.rootSection.playerMessages.pluginReloadedError.sendMessage(
        sender, config.rootSection.builtBaseEnvironment
      );
    }

    return ExitCode.SUCCESS;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, String[] args) {
    return List.of();
  }

  @Override
  public String getUsage(CommandSender sender) {
    return config.rootSection.playerMessages.commandReloadUsage.asScalar(
      ScalarType.STRING, config.rootSection.getBaseEnvironment()
        .withStaticVariable("sub_label", SubCommandLabel.matcher.getNormalizedName(label))
        .build()
    );
  }

  @Override
  public String getDescription(CommandSender sender) {
    return config.rootSection.playerMessages.commandReloadDescription.asScalar(
      ScalarType.STRING, config.rootSection.builtBaseEnvironment
    );
  }
}

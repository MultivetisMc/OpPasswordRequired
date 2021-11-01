package com.github.guardedoperators.opguard;

import com.github.guardedoperators.opguard.config.OpGuardConfig;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

final class CommandInterceptor implements Listener
{
    private final OpGuard api;
    
    public CommandInterceptor(OpGuard api)
    {
        this.api = api;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void on(PlayerCommandPreprocessEvent event)
    {
        String command = event.getMessage();
        
        if (intercept(event.getPlayer(), event.getMessage(), event))
        {
            event.setMessage(collapse(command));
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void on(ServerCommandEvent event)
    {
        String command = event.getCommand();
        
        if (intercept(event.getSender(), command, event))
        {
            event.setCommand(collapse(command));
        }
    }
    
    private String collapse(String command)
    {
        return "opguard:intercepted(" + command.replaceAll("/| .*", "") + ")";
    }
    
    private boolean intercept(CommandSender sender, String command, Event event)
    {
        String[] cmd = command.split(" ");
        String base = cmd[0].toLowerCase();
        
        if (!base.matches("(?i)^/?((minecraft:)?(de)?op|o(g|pguard))$"))
        {
            return false;
        }
        
        Context context = new Context(api).attemptFrom(sender);
        OpGuardConfig config = api.config();
        PluginStackChecker stack = new PluginStackChecker(api);
        
        if (stack.hasFoundPlugin())
        {
            String name = stack.getPlugin().getName();
            
            context.pluginAttempt().warning
            (
                "The plugin <!>" + name + "&f attempted to make &7" + sender.getName() +
                "&f execute <!>" + ((!command.startsWith("/")) ? "/" : "") + command
            );
            api.warn(context).log(context);
    
            stack.disablePlugin(api, context);
            return true;
        }
        else if (stack.hasAllowedPlugins())
        {
            Context allowed = context.copy();
            String name = stack.getTopAllowedPlugin().getName();
    
            allowed.okay
            (
                "The plugin &7" + name + "&f was allowed to execute &7" +
                ((!base.startsWith("/")) ? "/" : "") + base +
                "&f on behalf of &7" + sender.getName()
            );
            api.warn(allowed).log(allowed);
        }
        
        if (base.matches("(?i)^/?(minecraft:)?op$"))
        {
            context.setOp();
            
            if (cmd.length > 1)
            {
                String name = cmd[1];
                context.warning(sender.getName() + " attempted to " + base + " <!>" + name);
                api.warn(context).log(context).punish(context, name);
            }
        }
        else if (base.matches("(?i)^/?o(g|pguard)$"))
        {
            boolean isVerifiedOperator = sender.isOp() && api.verifier().isVerified(sender);
            boolean isPermitted = sender.hasPermission("opguard.manage") && api.config().isManagementPermissionEnabled();
            
            if (isVerifiedOperator || isPermitted)
            {
                api.run(sender, cmd);
    
                if (event instanceof Cancellable)
                {
                    ((Cancellable) event).setCancelled(true);
                }
            }
            else
            {
                context.incorrectlyUsedOpGuard().warning(sender.getName() + " attempted to access OpGuard");
                api.warn(context).log(context);
            }
        }
        return true;
    }
}
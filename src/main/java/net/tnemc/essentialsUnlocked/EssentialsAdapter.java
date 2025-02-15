package net.tnemc.essentialsUnlocked;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.OfflinePlayerStub;
import com.earth2me.essentials.User;
import com.earth2me.essentials.api.NoLoanPermittedException;
import com.earth2me.essentials.api.UserDoesNotExistException;
import com.earth2me.essentials.config.EssentialsUserConfiguration;
import com.earth2me.essentials.utils.AdventureUtil;
import com.earth2me.essentials.utils.NumberUtil;
import net.ess3.api.MaxMoneyException;
import net.milkbowl.vault2.economy.AccountPermission;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class EssentialsAdapter implements Economy {

  private static final String WARN_NPC_RECREATE_1 = "Account creation was requested for NPC user {0}, but an account file with UUID {1} already exists.";
  private static final String WARN_NPC_RECREATE_2 = "Essentials will create a new account as requested by the other plugin, but this is almost certainly a bug and should be reported.";

  private final Essentials ess;

  public EssentialsAdapter(final Essentials essentials) {
    this.ess = essentials;
  }

  @Override
  public boolean isEnabled() {

    return true;
  }

  @Override
  public @NotNull String getName() {

    return "EssentialsX Unlocked";
  }

  @Override
  public boolean hasSharedAccountSupport() {

    return false;
  }

  @Override
  public boolean hasMultiCurrencySupport() {

    return false;
  }

  @Override
  public int fractionalDigits(@NotNull final String pluginName) {

    return -1;
  }

  /**
   * @deprecated
   */
  @Override
  public @NotNull String format(@NotNull final BigDecimal amount) {

    return format(getName(), amount);
  }

  @Override
  public @NotNull String format(@NotNull final String pluginName, @NotNull final BigDecimal amount) {

    return AdventureUtil.miniToLegacy(NumberUtil.displayCurrency(amount, ess));
  }

  /**
   * @deprecated
   */
  @Override
  public @NotNull String format(@NotNull final BigDecimal amount, @NotNull final String currency) {

    return format(getName(), amount);
  }

  @Override
  public @NotNull String format(@NotNull final String pluginName, @NotNull final BigDecimal amount, @NotNull final String currency) {

    return format(pluginName, amount);
  }

  @Override
  public boolean hasCurrency(@NotNull final String currency) {

    return true;
  }

  @Override
  public @NotNull String getDefaultCurrency(@NotNull final String pluginName) {

    return defaultCurrencyNameSingular(pluginName);
  }

  @Override
  public @NotNull String defaultCurrencyNamePlural(@NotNull final String pluginName) {

    return defaultCurrencyNameSingular(pluginName);
  }

  @Override
  public @NotNull String defaultCurrencyNameSingular(@NotNull final String pluginName) {

    return ess.getSettings().getCurrencySymbol();
  }

  @Override
  public @NotNull Collection<String> currencies() {

    return Collections.singletonList(defaultCurrencyNameSingular(""));
  }

  /**
   * @deprecated
   */
  @Override
  public boolean createAccount(@NotNull final UUID accountID, @NotNull final String name) {

    //deprecated so don't implement
    return false;
  }

  @Override
  public boolean createAccount(@NotNull final UUID accountID, @NotNull final String name, final boolean player) {
    if(hasAccount(accountID)) {

      return false;
    }

    // String based UUIDs are version 3 and are used for NPC and OfflinePlayers
    // Citizens uses v2 UUIDs, yeah I don't know either!
    if(!player) {

      final File folder = new File(ess.getDataFolder(), "userdata");
      if(!folder.exists() && !folder.mkdirs()) {

        throw new RuntimeException("Error while creating userdata directory!");
      }

      final File npcFile = new File(folder, accountID + ".yml");
      if(npcFile.exists()) {

        ess.getLogger().log(Level.SEVERE, MessageFormat.format(WARN_NPC_RECREATE_1, name, accountID.toString()), new RuntimeException());
        ess.getLogger().log(Level.SEVERE, WARN_NPC_RECREATE_2);
      }

      final EssentialsUserConfiguration npcConfig = new EssentialsUserConfiguration(name, accountID, npcFile);

      npcConfig.load();
      npcConfig.setProperty("npc", true);
      npcConfig.setProperty("last-account-name", name);
      npcConfig.setProperty("money", ess.getSettings().getStartingBalance());
      npcConfig.blockingSave();

      //This will load the NPC into the UserMap + UUID cache
      ess.getUsers().addCachedNpcName(accountID, name);
      ess.getUsers().getUser(accountID);
      return true;
    }

    //If a player make sure Essentials tracks it, probably won't happen
    final OfflinePlayerStub essPlayer = new OfflinePlayerStub(accountID, ess.getServer());
    essPlayer.setName(name);

    ess.getUsers().getUser(essPlayer);
    return false;
  }

  /**
   * @deprecated
   */
  @Override
  public boolean createAccount(@NotNull final UUID accountID, @NotNull final String name, @NotNull final String worldName) {

    //deprecated so don't implement
    return false;
  }

  @Override
  public boolean createAccount(@NotNull final UUID accountID, @NotNull final String name, @NotNull final String worldName, final boolean player) {
    return createAccount(accountID, name, player);
  }

  @Override
  public @NotNull Map<UUID, String> getUUIDNameMap() {

    return ess.getUsers().getNameCache().entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
  }

  @Override
  public Optional<String> getAccountName(@NotNull final UUID accountID) {

    if(!hasAccount(accountID)) {

      return Optional.empty();
    }

    return Optional.of(ess.getUsers().getUser(accountID).getName());
  }

  @Override
  public boolean hasAccount(@NotNull final UUID accountID) {

    return com.earth2me.essentials.api.Economy.playerExists(accountID);
  }

  @Override
  public boolean hasAccount(@NotNull final UUID accountID, @NotNull final String worldName) {

    return hasAccount(accountID);
  }

  @Override
  public boolean renameAccount(@NotNull final UUID accountID, @NotNull final String name) {

    return renameAccount(getName(), accountID, name);
  }

  @Override
  public boolean renameAccount(@NotNull final String plugin, @NotNull final UUID accountID, @NotNull final String name) {

    final User user = ess.getUser(accountID);
    if(!user.isNPC()) {

      //cannot rename player.
      return false;
    }

    user.setLastAccountName(name);
    return true;
  }

  @Override
  public boolean deleteAccount(@NotNull final String plugin, @NotNull final UUID accountID) {

    final User user = ess.getUser(accountID);
    if(user.isNPC()) {

      user.reset();
      return true;
    } else {
      try {
        com.earth2me.essentials.api.Economy.resetBalance(accountID);

        return true;
      } catch(final NoLoanPermittedException | UserDoesNotExistException | MaxMoneyException  ignore) {

        return false;
      }
    }
  }

  @Override
  public boolean accountSupportsCurrency(@NotNull final String plugin, @NotNull final UUID accountID, @NotNull final String currency) {

    //No multi-currency support so return false
    return false;
  }

  @Override
  public boolean accountSupportsCurrency(@NotNull final String plugin, @NotNull final UUID accountID, @NotNull final String currency, @NotNull final String world) {

    //No multi-currency support so return false
    return false;
  }

  @Override
  public @NotNull BigDecimal balance(@NotNull final String pluginName, @NotNull final UUID accountID) {

    try {

      return com.earth2me.essentials.api.Economy.getMoneyExact(accountID);

    } catch (final UserDoesNotExistException ignore) {

      final Player player = Bukkit.getPlayer(accountID);
      if(player != null) {


        createAccount(accountID, player.getName(), true);

        return ess.getSettings().getStartingBalance();
      }
    }

    return BigDecimal.ZERO;
  }

  @Override
  public @NotNull BigDecimal balance(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String world) {

    return balance(pluginName, accountID);
  }

  @Override
  public @NotNull BigDecimal balance(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String world, @NotNull final String currency) {

    return balance(pluginName, accountID);
  }

  /**
   * @deprecated
   */
  @Override
  public @NotNull BigDecimal getBalance(@NotNull final String pluginName, @NotNull final UUID accountID) {

    return balance(pluginName, accountID);
  }

  /**
   * @deprecated
   */
  @Override
  public @NotNull BigDecimal getBalance(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String world) {

    return balance(pluginName, accountID);
  }

  /**
   * @deprecated
   */
  @Override
  public @NotNull BigDecimal getBalance(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String world, @NotNull final String currency) {

    return balance(pluginName, accountID);
  }

  @Override
  public boolean has(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final BigDecimal amount) {

    return balance(pluginName, accountID).compareTo(amount) >= 0;
  }

  @Override
  public boolean has(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String worldName, @NotNull final BigDecimal amount) {

    return balance(pluginName, accountID).compareTo(amount) >= 0;
  }

  @Override
  public boolean has(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String worldName, @NotNull final String currency, @NotNull final BigDecimal amount) {

    return balance(pluginName, accountID).compareTo(amount) >= 0;
  }

  @Override
  public @NotNull EconomyResponse withdraw(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final BigDecimal amount) {

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {

      return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE, "Cannot withdraw negative funds!");
    }

    try {

      com.earth2me.essentials.api.Economy.subtract(accountID, amount);

      return new EconomyResponse(amount, balance(getName(), accountID), EconomyResponse.ResponseType.SUCCESS, "Success!");

    } catch (final UserDoesNotExistException ignore) {

      return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE, "User does not exist!");

    } catch (final NoLoanPermittedException ignore) {

      return new EconomyResponse(BigDecimal.ZERO, balance(getName(), accountID), EconomyResponse.ResponseType.FAILURE, "Loan was not permitted!");

    } catch (final MaxMoneyException ignore) {

      return new EconomyResponse(BigDecimal.ZERO, balance(getName(), accountID), EconomyResponse.ResponseType.FAILURE, "User goes over maximum money limit!");
    }
  }

  @Override
  public @NotNull EconomyResponse withdraw(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String worldName, @NotNull final BigDecimal amount) {

    return withdraw(pluginName, accountID, amount);
  }

  @Override
  public @NotNull EconomyResponse withdraw(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String worldName, @NotNull final String currency, @NotNull final BigDecimal amount) {

    return withdraw(pluginName, accountID, amount);
  }

  @Override
  public @NotNull EconomyResponse deposit(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final BigDecimal amount) {

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {

      return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE, "Cannot deposit negative funds!");
    }

    try {

      com.earth2me.essentials.api.Economy.add(accountID, amount);
      return new EconomyResponse(amount, balance(getName(), accountID), EconomyResponse.ResponseType.SUCCESS, "Success!");

    } catch (final UserDoesNotExistException ignore) {

      return new EconomyResponse(BigDecimal.ZERO, BigDecimal.ZERO, EconomyResponse.ResponseType.FAILURE, "User does not exist!");

    } catch (final NoLoanPermittedException ignore) {

      return new EconomyResponse(BigDecimal.ZERO, balance(getName(), accountID), EconomyResponse.ResponseType.FAILURE, "Loan was not permitted!");

    } catch (final MaxMoneyException ignore) {

      return new EconomyResponse(BigDecimal.ZERO, balance(getName(), accountID), EconomyResponse.ResponseType.FAILURE, "User goes over maximum money limit!");
    }
  }

  @Override
  public @NotNull EconomyResponse deposit(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String worldName, @NotNull final BigDecimal amount) {

    return deposit(pluginName, accountID, amount);
  }

  @Override
  public @NotNull EconomyResponse deposit(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String worldName, @NotNull final String currency, @NotNull final BigDecimal amount) {

    return deposit(pluginName, accountID, amount);
  }

  /*
   * Essentials doesn't support the shared account stuff so don't implement the methods below
   */
  @Override
  public boolean createSharedAccount(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final String name, @NotNull final UUID owner) {

    return false;
  }

  @Override
  public boolean isAccountOwner(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final UUID uuid) {

    return false;
  }

  @Override
  public boolean setOwner(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final UUID uuid) {

    return false;
  }

  @Override
  public boolean isAccountMember(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final UUID uuid) {

    return false;
  }

  @Override
  public boolean addAccountMember(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final UUID uuid) {

    return false;
  }

  @Override
  public boolean addAccountMember(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final UUID uuid, final @NotNull AccountPermission... initialPermissions) {

    return false;
  }

  @Override
  public boolean removeAccountMember(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final UUID uuid) {

    return false;
  }

  @Override
  public boolean hasAccountPermission(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final UUID uuid, @NotNull final AccountPermission permission) {

    return false;
  }

  @Override
  public boolean updateAccountPermission(@NotNull final String pluginName, @NotNull final UUID accountID, @NotNull final UUID uuid, @NotNull final AccountPermission permission, final boolean value) {

    return false;
  }
}
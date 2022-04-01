/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.core.payment;

import bisq.core.account.witness.AccountAgeWitnessService;
import bisq.core.locale.Country;
import bisq.core.locale.CurrencyUtil;
import bisq.core.locale.TradeCurrency;
import bisq.core.offer.Offer;
import bisq.core.payment.payload.PaymentAccountPayload;
import bisq.core.payment.payload.PaymentMethod;
import bisq.core.user.User;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

import static bisq.core.locale.CurrencyUtil.*;
import static bisq.core.payment.payload.PaymentMethod.*;
import static java.util.Comparator.comparing;

@Slf4j
public class PaymentAccountUtil {

    public static boolean isAnyPaymentAccountValidForOffer(Offer offer,
                                                           Collection<PaymentAccount> paymentAccounts) {
        for (PaymentAccount paymentAccount : paymentAccounts) {
            if (isPaymentAccountValidForOffer(offer, paymentAccount))
                return true;
        }
        return false;
    }

    public static ObservableList<PaymentAccount> getPossiblePaymentAccounts(Offer offer,
                                                                            Set<PaymentAccount> paymentAccounts,
                                                                            AccountAgeWitnessService accountAgeWitnessService) {
        ObservableList<PaymentAccount> result = FXCollections.observableArrayList();
        result.addAll(paymentAccounts.stream()
                .filter(paymentAccount -> isPaymentAccountValidForOffer(offer, paymentAccount))
                .filter(paymentAccount -> isAmountValidForOffer(offer, paymentAccount, accountAgeWitnessService))
                .collect(Collectors.toList()));
        return result;
    }

    // Return true if paymentAccount can take this offer
    public static boolean isAmountValidForOffer(Offer offer,
                                                PaymentAccount paymentAccount,
                                                AccountAgeWitnessService accountAgeWitnessService) {
        boolean hasChargebackRisk = PaymentMethod.hasChargebackRisk(offer.getPaymentMethod(), offer.getCurrencyCode());
        boolean hasValidAccountAgeWitness = accountAgeWitnessService.getMyTradeLimit(paymentAccount,
                offer.getCurrencyCode(), offer.getMirroredDirection()) >= offer.getMinAmount().value;
        return !hasChargebackRisk || hasValidAccountAgeWitness;
    }

    // TODO might be used to show more details if we get payment methods updates with diff. limits
    public static String getInfoForMismatchingPaymentMethodLimits(Offer offer, PaymentAccount paymentAccount) {
        // don't translate atm as it is not used so far in the UI just for logs
        return "Payment methods have different trade limits or trade periods.\n" +
                "Our local Payment method: " + paymentAccount.getPaymentMethod().toString() + "\n" +
                "Payment method from offer: " + offer.getPaymentMethod().toString();
    }

    public static boolean isPaymentAccountValidForOffer(Offer offer, PaymentAccount paymentAccount) {
        return new ReceiptValidator(offer, paymentAccount).isValid();
    }

    public static Optional<PaymentAccount> getMostMaturePaymentAccountForOffer(Offer offer,
                                                                               Set<PaymentAccount> paymentAccounts,
                                                                               AccountAgeWitnessService service) {
        PaymentAccounts accounts = new PaymentAccounts(paymentAccounts, service);
        return Optional.ofNullable(accounts.getOldestPaymentAccountForOffer(offer));
    }

    @Nullable
    public static ArrayList<String> getAcceptedCountryCodes(PaymentAccount paymentAccount) {
        ArrayList<String> acceptedCountryCodes = null;
        if (paymentAccount instanceof SepaAccount) {
            acceptedCountryCodes = new ArrayList<>(((SepaAccount) paymentAccount).getAcceptedCountryCodes());
        } else if (paymentAccount instanceof SepaInstantAccount) {
            acceptedCountryCodes = new ArrayList<>(((SepaInstantAccount) paymentAccount).getAcceptedCountryCodes());
        } else if (paymentAccount instanceof CountryBasedPaymentAccount) {
            acceptedCountryCodes = new ArrayList<>();
            Country country = ((CountryBasedPaymentAccount) paymentAccount).getCountry();
            if (country != null)
                acceptedCountryCodes.add(country.code);
        }
        return acceptedCountryCodes;
    }

    public static List<TradeCurrency> getTradeCurrencies(PaymentMethod paymentMethod) {
        switch (paymentMethod.getId()) {
            case ADVANCED_CASH_ID:
                return getAllAdvancedCashCurrencies();
            case AMAZON_GIFT_CARD_ID:
                return getAllAmazonGiftCardCurrencies();
            case CAPITUAL_ID:
                return getAllCapitualCurrencies();
            case MONEY_GRAM_ID:
                return getAllMoneyGramCurrencies();
            case PAXUM_ID:
                return getAllPaxumCurrencies();
            case PAYSERA_ID:
                return getAllPayseraCurrencies();
            case REVOLUT_ID:
                return getAllRevolutCurrencies();
            case SWIFT_ID:
                return new ArrayList<>(getAllSortedFiatCurrencies(
                        comparing(TradeCurrency::getCode)));
            case TRANSFERWISE_ID:
                return getAllTransferwiseCurrencies();
            case UPHOLD_ID:
                return getAllUpholdCurrencies();
            case INTERAC_E_TRANSFER_ID:
                return getAllInteracETransferIdCurrencies();
            case STRIKE_ID:
                return getAllStrikeCurrencies();
            case TIKKIE_ID:
                return CurrencyUtil.getAllTikkieIdCurrencies();
            case ALI_PAY_ID:
                return CurrencyUtil.getAllAliPayAccountCurrencies();
            case NEQUI_ID:
                return CurrencyUtil.getAllNequiCurrencies();
            case IMPS_ID:
            case NEFT_ID:
            case PAYTM_ID:
            case RTGS_ID:
            case UPI_ID:
                return CurrencyUtil.getAllIfscBankCurrencies();
            case BIZUM_ID:
                return CurrencyUtil.getAllBizumCurrencies();
            case MONEY_BEAM_ID:
                return CurrencyUtil.getAllMoneyBeamCurrencies();
            case PIX_ID:
                return CurrencyUtil.getAllPixCurrencies();
            case SATISPAY_ID:
                return CurrencyUtil.getAllSatispayCurrencies();
            case CHASE_QUICK_PAY_ID:
                return CurrencyUtil.getAllChaseQuickPayCurrencies();
            case US_POSTAL_MONEY_ORDER_ID:
                return CurrencyUtil.getAllUSPostalMoneyOrderCurrencies();
            case VENMO_ID:
                return CurrencyUtil.getAllVenmoCurrencies();
            case JAPAN_BANK_ID:
                return CurrencyUtil.getAllJapanBankCurrencies();
            case WECHAT_PAY_ID:
                return CurrencyUtil.getAllWeChatPayCurrencies();
            case CLEAR_X_CHANGE_ID:
                return CurrencyUtil.getAllClearXchangeCurrencies();
            case AUSTRALIA_PAYID_ID:
                return CurrencyUtil.getAllAustraliaPayidCurrencies();
            case PERFECT_MONEY_ID:
                return CurrencyUtil.getAllPerfectMoneyCurrencies();
            case HAL_CASH_ID:
                return CurrencyUtil.getAllHalCashCurrencies();
            case SWISH_ID:
                return CurrencyUtil.getAllSwishCurrencies();
            case CASH_APP_ID:
                return CurrencyUtil.getAllCashAppCurrencies();
            case POPMONEY_ID:
                return CurrencyUtil.getAllPopmoneyCurrencies();
            case PROMPT_PAY_ID:
                return CurrencyUtil.getAllPromptPayCurrencies();
            case SEPA_ID:
            case SEPA_INSTANT_ID:
                return CurrencyUtil.getAllSEPACurrencies();
            case CASH_BY_MAIL_ID:
            case F2F_ID:
            case NATIONAL_BANK_ID:
            case SAME_BANK_ID:
            case SPECIFIC_BANKS_ID:
            case CASH_DEPOSIT_ID:
                return CurrencyUtil.getAllFiatCurrencies();
            case FASTER_PAYMENTS_ID:
                return CurrencyUtil.getAllFasterPaymentCurrencies();
                case DOMESTIC_WIRE_TRANSFER_ID:
                    return CurrencyUtil.getAllDomesticWireTransferCurrencies();
            default:
                return Collections.emptyList();
        }
    }

    public static boolean supportsCurrency(PaymentMethod paymentMethod, TradeCurrency selectedTradeCurrency) {
        return getTradeCurrencies(paymentMethod).stream()
                .anyMatch(tradeCurrency -> tradeCurrency.equals(selectedTradeCurrency));
    }

    @Nullable
    public static List<String> getAcceptedBanks(PaymentAccount paymentAccount) {
        List<String> acceptedBanks = null;
        if (paymentAccount instanceof SpecificBanksAccount) {
            acceptedBanks = new ArrayList<>(((SpecificBanksAccount) paymentAccount).getAcceptedBanks());
        } else if (paymentAccount instanceof SameBankAccount) {
            acceptedBanks = new ArrayList<>();
            acceptedBanks.add(((SameBankAccount) paymentAccount).getBankId());
        }
        return acceptedBanks;
    }

    @Nullable
    public static String getBankId(PaymentAccount paymentAccount) {
        return paymentAccount instanceof BankAccount ? ((BankAccount) paymentAccount).getBankId() : null;
    }

    @Nullable
    public static String getCountryCode(PaymentAccount paymentAccount) {
        // That is optional and set to null if not supported (AltCoins,...)
        if (paymentAccount instanceof CountryBasedPaymentAccount) {
            Country country = (((CountryBasedPaymentAccount) paymentAccount)).getCountry();
            return country != null ? country.code : null;
        }
        return null;
    }

    public static boolean isCryptoCurrencyAccount(PaymentAccount paymentAccount) {
        return (paymentAccount != null && paymentAccount.getPaymentMethod().equals(PaymentMethod.BLOCK_CHAINS) ||
                paymentAccount != null && paymentAccount.getPaymentMethod().equals(PaymentMethod.BLOCK_CHAINS_INSTANT));
    }

    public static Optional<PaymentAccount> findPaymentAccount(PaymentAccountPayload paymentAccountPayload,
                                                              User user) {
        return user.getPaymentAccountsAsObservable().stream().
                filter(e -> e.getPaymentAccountPayload().equals(paymentAccountPayload))
                .findAny();
    }
}

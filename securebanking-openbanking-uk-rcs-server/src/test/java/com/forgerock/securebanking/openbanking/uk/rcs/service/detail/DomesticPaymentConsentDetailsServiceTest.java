/**
 * Copyright © 2020 ForgeRock AS (obst@forgerock.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.forgerock.securebanking.openbanking.uk.rcs.service.detail;

import com.forgerock.securebanking.common.openbanking.uk.forgerock.datamodel.account.FRAccountWithBalance;
import com.forgerock.securebanking.common.openbanking.uk.forgerock.datamodel.payment.FRAccountIdentifier;
import com.forgerock.securebanking.common.openbanking.uk.forgerock.datamodel.tpp.Tpp;
import com.forgerock.securebanking.openbanking.uk.error.OBErrorException;
import com.forgerock.securebanking.openbanking.uk.rcs.api.dto.consent.details.DomesticPaymentConsentDetails;
import com.forgerock.securebanking.openbanking.uk.rcs.client.idm.PaymentConsentService;
import com.forgerock.securebanking.openbanking.uk.rcs.client.idm.TppService;
import com.forgerock.securebanking.openbanking.uk.rcs.client.idm.dto.consent.FRDomesticPaymentConsent;
import com.forgerock.securebanking.openbanking.uk.rcs.exception.InvalidConsentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.org.openbanking.datamodel.payment.OBWriteDomestic2DataInitiation;

import java.util.List;
import java.util.Optional;

import static com.forgerock.securebanking.common.openbanking.uk.forgerock.datamodel.converter.FRAmountConverter.toFRAmount;
import static com.forgerock.securebanking.common.openbanking.uk.forgerock.datamodel.testsupport.FRAccountIdentifierTestDataFactory.aValidFRAccountIdentifier2;
import static com.forgerock.securebanking.common.openbanking.uk.forgerock.datamodel.testsupport.account.FRAccountWithBalanceTestDataFactory.aValidFRAccountWithBalance;
import static com.forgerock.securebanking.common.openbanking.uk.forgerock.datamodel.testsupport.account.FRFinancialAccountTestDataFactory.aValidFRFinancialAccountBuilder;
import static com.forgerock.securebanking.common.openbanking.uk.forgerock.datamodel.testsupport.tpp.TppTestDataFactory.aValidTppBuilder;
import static com.forgerock.securebanking.openbanking.uk.error.OBRIErrorType.*;
import static com.forgerock.securebanking.openbanking.uk.rcs.testsupport.ConsentDetailsRequestTestDataFactory.aValidDomesticPaymentConsentDetailsRequest;
import static com.forgerock.securebanking.openbanking.uk.rcs.testsupport.ConsentDetailsRequestTestDataFactory.aValidDomesticPaymentConsentDetailsRequestBuilder;
import static com.forgerock.securebanking.openbanking.uk.rcs.testsupport.idm.dto.consent.FRDomesticPaymentConsentDataTestDataFactory.aValidDomesticPaymentConsentDataBuilder;
import static com.forgerock.securebanking.openbanking.uk.rcs.testsupport.idm.dto.consent.FRDomesticPaymentConsentTestDataFactory.aValidFRDomesticPaymentConsentBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.org.openbanking.testsupport.payment.OBWriteDomesticConsentTestDataFactory.aValidOBWriteDomestic2DataInitiation;

/**
 * Unit test for {@link DomesticPaymentConsentDetailsService}.
 */
@ExtendWith(MockitoExtension.class)
public class DomesticPaymentConsentDetailsServiceTest {
    @Mock
    private PaymentConsentService paymentConsentService;
    @Mock
    private TppService tppService;
    @InjectMocks
    private DomesticPaymentConsentDetailsService consentDetailsService;

    @Test
    public void shouldGetConsentDetails() throws OBErrorException {
        // Given
        OBWriteDomestic2DataInitiation initiation = aValidOBWriteDomestic2DataInitiation();
        ConsentDetailsRequest request = aValidConsentDetailsRequest(initiation);
        FRDomesticPaymentConsent consent = aValidFRDomesticPaymentConsentBuilder(request.getIntentId())
                .oauth2ClientId(request.getClientId())
                .data(aValidDomesticPaymentConsentDataBuilder(request.getIntentId()).initiation(initiation).build())
                .build();
        Tpp tpp = aValidTppBuilder()
                .clientId(request.getClientId())
                .build();
        given(paymentConsentService.getConsent(request.getIntentId(), FRDomesticPaymentConsent.class)).willReturn(consent);
        given(tppService.getTpp(consent.getOauth2ClientId())).willReturn(Optional.of(tpp));

        // When
        DomesticPaymentConsentDetails consentDetails = (DomesticPaymentConsentDetails) consentDetailsService.getConsentDetails(request);

        // Then
        assertThat(consentDetails).isNotNull();
        assertThat(consent.getResourceOwnerUsername()).isEqualTo(request.getUsername());
        assertThat(consentDetails.getInstructedAmount()).isEqualTo(toFRAmount(initiation.getInstructedAmount()));
        assertThat(consentDetails.getAccounts()).isEqualTo(request.getAccounts());
        assertThat(consentDetails.getUsername()).isEqualTo(request.getUsername());
        assertThat(consentDetails.getMerchantName()).isEqualTo(consent.getOauth2ClientName());
        assertThat(consentDetails.getClientId()).isEqualTo(request.getClientId());
        verify(paymentConsentService).updateConsent(consent);
    }

    @Test
    public void shouldFailToGetConsentDetailsGivenPaymentConsentNotFound() {
        // Given
        ConsentDetailsRequest request = aValidDomesticPaymentConsentDetailsRequest();
        given(paymentConsentService.getConsent(request.getIntentId(), FRDomesticPaymentConsent.class)).willReturn(null);

        // When
        OBErrorException e = catchThrowableOfType(() -> consentDetailsService.getConsentDetails(request), OBErrorException.class);

        // Then
        assertThat(e.getObriErrorType()).isEqualTo(PAYMENT_CONSENT_NOT_FOUND);
    }

    @Test
    public void shouldFailToGetConsentDetailsGivenDebtorAccountDoesNotBelongToUser() {
        // Given
        FRAccountWithBalance accountWithBalance = aValidFRAccountWithBalance();
        accountWithBalance.setAccount(aValidFRFinancialAccountBuilder()
                .accounts(List.of(aValidFRAccountIdentifier2()))
                .build());
        ConsentDetailsRequest request = aValidDomesticPaymentConsentDetailsRequestBuilder()
                .accounts(List.of(accountWithBalance))
                .build();
        FRDomesticPaymentConsent consent = aValidFRDomesticPaymentConsentBuilder()
                .build();
        given(paymentConsentService.getConsent(request.getIntentId(), FRDomesticPaymentConsent.class)).willReturn(consent);

        // When
        InvalidConsentException e = catchThrowableOfType(() -> consentDetailsService.getConsentDetails(request), InvalidConsentException.class);

        // Then
        assertThat(e.getErrorType()).isEqualTo(RCS_CONSENT_REQUEST_DEBTOR_ACCOUNT_NOT_FOUND);
    }

    @Test
    public void shouldFailToGetConsentDetailsGivenTppNotFound() {
        // Given
        OBWriteDomestic2DataInitiation initiation = aValidOBWriteDomestic2DataInitiation();
        ConsentDetailsRequest request = aValidConsentDetailsRequest(initiation);
        FRDomesticPaymentConsent consent = aValidFRDomesticPaymentConsentBuilder(request.getIntentId())
                .oauth2ClientId(request.getClientId())
                .data(aValidDomesticPaymentConsentDataBuilder(request.getIntentId()).initiation(initiation).build())
                .build();
        given(paymentConsentService.getConsent(request.getIntentId(), FRDomesticPaymentConsent.class)).willReturn(consent);
        given(tppService.getTpp(consent.getOauth2ClientId())).willReturn(Optional.empty());

        // When
        InvalidConsentException e = catchThrowableOfType(() -> consentDetailsService.getConsentDetails(request), InvalidConsentException.class);

        // Then
        assertThat(e.getErrorType()).isEqualTo(RCS_CONSENT_REQUEST_NOT_FOUND_TPP);
    }

    @Test
    public void shouldFailToGetConsentDetailsGivenTppDidNotCreateConsent() {
        // Given
        OBWriteDomestic2DataInitiation initiation = aValidOBWriteDomestic2DataInitiation();
        ConsentDetailsRequest request = aValidConsentDetailsRequest(initiation);
        FRDomesticPaymentConsent consent = aValidFRDomesticPaymentConsentBuilder(request.getIntentId())
                .oauth2ClientId(request.getClientId())
                .data(aValidDomesticPaymentConsentDataBuilder(request.getIntentId()).initiation(initiation).build())
                .build();
        Tpp tpp = aValidTppBuilder().build(); // different client ID to consent
        given(paymentConsentService.getConsent(request.getIntentId(), FRDomesticPaymentConsent.class)).willReturn(consent);
        given(tppService.getTpp(consent.getOauth2ClientId())).willReturn(Optional.of(tpp));

        // When
        OBErrorException e = catchThrowableOfType(() -> consentDetailsService.getConsentDetails(request), OBErrorException.class);

        // Then
        assertThat(e.getObriErrorType()).isEqualTo(RCS_CONSENT_REQUEST_INVALID_PAYMENT_REQUEST);
    }

    private ConsentDetailsRequest aValidConsentDetailsRequest(OBWriteDomestic2DataInitiation initiation) {
        FRAccountWithBalance accountWithBalance = aValidFRAccountWithBalance();
        FRAccountIdentifier accountIdentifier = accountWithBalance.getAccount().getAccounts().get(0);
        accountIdentifier.setIdentification(initiation.getDebtorAccount().getIdentification());
        return aValidDomesticPaymentConsentDetailsRequestBuilder()
                .accounts(List.of(accountWithBalance))
                .build();
    }
}
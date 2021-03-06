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
package com.forgerock.securebanking.openbanking.uk.rcs.client.idm;

import com.forgerock.securebanking.common.openbanking.uk.forgerock.datamodel.tpp.Tpp;
import com.forgerock.securebanking.openbanking.uk.rcs.configuration.RcsConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
@Slf4j
public class TppService {
    private static final String TPP_URI = "/repo";

    private final RcsConfigurationProperties configurationProperties;
    private final RestTemplate restTemplate;

    public TppService(RcsConfigurationProperties configurationProperties, RestTemplate restTemplate) {
        this.configurationProperties = configurationProperties;
        this.restTemplate = restTemplate;
    }

    public Optional<Tpp> getTpp(String tppId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        // TODO - add additional required headers
        ResponseEntity<Tpp> response = restTemplate.getForEntity(tppIdUrl(tppId), Tpp.class, headers);
        if (response.getStatusCode().equals(OK) && response.getBody() != null) {
            return Optional.of(response.getBody());
        }
        return Optional.empty();
    }

    private String tppUrl() {
        return configurationProperties.getIdmBaseUrl() + TPP_URI;
    }

    private String tppIdUrl(String consentId) {
        return tppUrl() + "/" + consentId;
    }
}

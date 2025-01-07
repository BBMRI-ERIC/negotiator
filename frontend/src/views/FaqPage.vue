<template>
  <h1 class="text-center mb-5" :style="{'color': uiConfiguration?.primaryTextColor}">
    Support
  </h1>
  <div class="row row-cols-1 row-cols-md-2 d-grid-row">
    <div class="mb-4">
      <div class="card">
        <h5 class="card-header" :style="{'color': uiConfiguration?.primaryTextColor}">
          Contact
        </h5>
        <div class="card-body">
          <h5 class="card-title" :style="{'color': uiConfiguration?.primaryTextColor}">
            Contact negotiation support by email.
          </h5>
          <div class="mt-4">
            <a
                href="mailto:negotiator@helpdesk.bbmri-eric.eu"
                class="link-underline link-underline-opacity-0"
                :style="{'color': uiConfiguration?.linksColor}"
            ><i class="bi bi-envelope-at"/> Email</a>
          </div>
        </div>
      </div>
    </div>

    <div class="mb-4">
      <div class="card">
        <h5 class="card-header" :style="{'color': uiConfiguration?.primaryTextColor}">
          Feedback
        </h5>
        <div class="card-body">
          <h5 class="card-title" :style="{'color': uiConfiguration?.primaryTextColor}">
            We value your feedback to improve our services.
          </h5>
          <div class="mt-4">
            <a
                href="https://forms.office.com/e/Z3EvCnp6cw"
                :style="{'color': uiConfiguration?.linksColor}"
            >
              <i class="bi bi-box-arrow-up-right"/>
              Feedback form
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>

  <div class="mb-4">
    <div class="card">
      <h5 class="card-header" :style="{'color': uiConfiguration?.primaryTextColor}">
        FAQ
      </h5>
      <div class="card-body">
        <div
            id="accordionExample"
            class="accordion"
        >
          <div class="accordion-item">
            <h2 class="accordion-header">
              <button
                  class="accordion-button collapsed"
                  type="button"
                  data-bs-toggle="collapse"
                  data-bs-target="#collapseOne"
                  aria-expanded="false"
                  aria-controls="collapseOne"
                  :style="{'color': uiConfiguration?.primaryTextColor}"
              >
                I want direct communication with the biobanks to align my needs with their possibilities.
              </button>
            </h2>
            <div
                id="collapseOne"
                class="accordion-collapse collapse"
                data-bs-parent="#accordionExample"
                :style="{'color': uiConfiguration?.secondaryTextColor}"
            >
              <div class="accordion-body">
                BBMRI-ERIC provides its Negotiator as a platform to support researchers and biobankers in the complex
                communication necessary to achieve a consensus on conditions of samples and/or data delivery. Requests
                issued by researchers are becoming increasingly complex and biobanks and biomolecular resources also
                need to verify availability of the data and biological material for a particular purpose defined by the
                researcher. Negotiator allows effective streamlining and monitoring throughout the whole process of
                these negotiations.
              </div>
            </div>
          </div>
          <div class="accordion-item">
            <h2 class="accordion-header">
              <button
                  class="accordion-button collapsed"
                  type="button"
                  data-bs-toggle="collapse"
                  data-bs-target="#collapseTwo"
                  aria-expanded="false"
                  aria-controls="collapseTwo"
                  :style="{'color': uiConfiguration?.primaryTextColor}"
              >
                I want to browse all biobanks in the BBMRI-ERIC.
              </button>
            </h2>
            <div
                id="collapseTwo"
                class="accordion-collapse collapse"
                data-bs-parent="#accordionExample"
                :style="{'color': uiConfiguration?.secondaryTextColor}"
            >
              <div class="accordion-body">
                The Directory is a broad catalogue of our biobanks with aggregated information on their collections. You
                can browse and filter by country, sample type, quality marks, ICD-10 and more.
              </div>
            </div>
          </div>
          <div class="accordion-item">
            <h2 class="accordion-header">
              <button
                  class="accordion-button collapsed"
                  type="button"
                  data-bs-toggle="collapse"
                  data-bs-target="#collapseThree"
                  aria-expanded="false"
                  aria-controls="collapseThree"
                  :style="{'color': uiConfiguration?.primaryTextColor}"
              >
                I need tissue and liquid samples fitting to my research scope.
              </button>
            </h2>
            <div
                id="collapseThree"
                class="accordion-collapse collapse"
                data-bs-parent="#accordionExample"
                :style="{'color': uiConfiguration?.secondaryTextColor}"
            >
              <div class="accordion-body">
                The Locator is your access to the samples and data associated to the samples based on real-time search
                on donor and sample level.
              </div>
            </div>
          </div>
          <div class="accordion-item">
            <h2 class="accordion-header">
              <button
                  class="accordion-button collapsed"
                  type="button"
                  data-bs-toggle="collapse"
                  data-bs-target="#collapseFour"
                  aria-expanded="false"
                  aria-controls="collapseFour"
                  :style="{'color': uiConfiguration?.primaryTextColor}"
              >
                I am interested more in genomics and population data analysis than samples themselves.
              </button>
            </h2>
            <div
                id="collapseFour"
                class="accordion-collapse collapse"
                data-bs-parent="#accordionExample"
                :style="{'color': uiConfiguration?.secondaryTextColor}"
            >
              <div class="accordion-body">
                The Finder is a data analysis tool focused on clinical, phenotypic and genomics data.
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <h1 class="text-center mt-3 mb-5" :style="{'color': uiConfiguration?.primaryTextColor}">
    Learn
  </h1>

  <div class="row row-cols-1 row-cols-md-2 d-grid-row">
    <div class="mb-4">
      <div class="card" :style="{'color': uiConfiguration?.primaryTextColor}">
        <h5 class="card-header">
          Learn about negotiatior
        </h5>
        <div class="card-body">
          <h5 class="card-title">
            Learn more about the Negotiator and its use to communicate with BioBanks to obtain information on the
            availability of relevant samples/data.
          </h5>
          <div class="mt-4">
            <a href="https://www.bbmri-eric.eu/services/sample-negotiator/"
               :style="{'color': uiConfiguration?.linksColor}"> <i class="bi bi-box-arrow-up-right"/> Go to learn
              page</a>
          </div>
        </div>
      </div>
    </div>

    <div class="mb-4">
      <div class="card" :style="{'color': uiConfiguration?.primaryTextColor}">
        <h5 class="card-header">
          Federated Platform
        </h5>
        <div class="card-body">
          <h5 class="card-title">
            Information about the Federated Platform and its advanced services such as the federated search or data
            analysis platform.
          </h5>
          <div class="mt-5"/>
          <a href="https://www.bbmri-eric.eu/federated-platform/" :style="{'color': uiConfiguration?.linksColor}"> <i
              class="bi bi-box-arrow-up-right"/> Go to Federated Platform page</a>
        </div>
      </div>
    </div>

    <div class="mb-4">
      <div class="card" :style="{'color': uiConfiguration?.primaryTextColor}">
        <h5 class="card-header">
          BBMRI-ERIC Sample and Data portal
        </h5>
        <div class="card-body">
          <h5 class="card-title">
            Information about the BBMRI-ERIC access pipeline helping expedite access to samples and data.
          </h5>
          <div class="mt-4"/>
          <a href="https://www.bbmri-eric.eu/bbmri-sample-and-data-portal/"
             :style="{'color': uiConfiguration?.linksColor}"><i class="bi bi-box-arrow-up-right"/> Go to BBMRI-ERIC
            Sample and Data portal page</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed, onMounted} from "vue"
import {useUserStore} from "../store/user"
import {useUiConfiguration} from '@/store/uiConfiguration.js'

const uiConfigurationStore = useUiConfiguration()
const userStore = useUserStore()

onMounted(async () => {
  if (Object.keys(userStore.userInfo).length === 0) {
    await userStore.retrieveUser()
  }
})

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.theme
})
</script>

<style scoped>
a:link {
  text-decoration: none;
}
</style>
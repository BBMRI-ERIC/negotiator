<template>
  <v-tour name="myTour" :steps="steps" :callbacks="myCallbacks" />
</template>

<script>
export default {
  data() {
    return {
      myCallbacks: {
        onSkip: this.dontShowVueTour,
        onFinish: this.dontShowVueTour,
      },
      steps: [
        {
          target: '#v-step-0',
          header: {
            title: 'Welcome',
          },
          content:
            'In the <strong>Negotiator</strong>, you can view the status of your negotiations and stay in contact with the providers of the desired resources.',
        },
        {
          target: '#v-step-1',
          header: {
            title: 'Selection of the view.',
          },
          content:
            'You can present your enquiries in a compact table or an informative card layout.',
        },
        {
          target: '#v-step-2',
          header: {
            title: 'Status',
          },
          params: {
            placement: 'top',
          },
          content: 'You can see the current status of your enquiry at a glance.',
        },
        {
          target: '#v-step-3',
          header: {
            title: 'Filter',
          },
          content: 'You also have the option of sorting and filtering your negotiations.',
        },
      ],
    }
  },
  mounted: function () {
    // Do not display after the first visit so that returning users are not annoyed!
    if (!localStorage.getItem('show_vue_tour_1')) {
      this.$tours.myTour.start()
    }
  },
  methods: {
    dontShowVueTour() {
      localStorage.setItem('show_vue_tour_1', true)
    },
  },
}
</script>

<style lang="scss">
.v-step {
  min-width: 420px !important;
  padding: 20px !important;
  .v-step__header {
    font-size: 24px;
    font-weight: bold;
  }
}
/* step-1 */
.v-step[id='v-step-46f34eda'] {
  background-color: #63e6be !important;
  color: #183153 !important;

  .v-step__arrow,
  .v-step__arrow--dark:before {
    background-color: #63e6be !important;
  }
  .v-step__header {
    background-color: #63e6be !important;
  }
  .v-step__button {
    color: #183153 !important;
    border: 0.05rem solid #183153 !important;
  }
}
/* step-2 */
.v-step[id='v-step-46f34ed8'] {
  background-color: #74c0fc !important;
  color: #183153 !important;

  .v-step__arrow,
  .v-step__arrow:before {
    background-color: #74c0fc !important;
  }
  .v-step__header {
    background-color: #74c0fc !important;
  }
  .v-step__button {
    color: #183153 !important;
    border: 0.05rem solid #183153 !important;
  }
}

/* step-3 */
.v-step[id='v-step-46f34ed6'] {
  background-color: #ffd43b !important;
  color: #183153 !important;

  .v-step__arrow,
  .v-step__arrow:before {
    background-color: #ffd43b !important;
  }
  .v-step__header {
    background-color: #ffd43b !important;
  }
  .v-step__button {
    color: #183153 !important;
    border: 0.05rem solid #183153 !important;
  }
}

/* step-4 */
.v-step[id='v-step-46f34ed4'] {
  background-color: #63e6be !important;
  color: #183153 !important;

  .v-step__arrow,
  .v-step__arrow--dark:before {
    background-color: #63e6be !important;
  }
  .v-step__header {
    background-color: #63e6be !important;
  }
  .v-step__button {
    color: #183153 !important;
    border: 0.05rem solid #183153 !important;
  }
}
</style>

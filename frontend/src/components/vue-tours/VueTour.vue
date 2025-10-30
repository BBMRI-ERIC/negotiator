<template>
  <v-tour name="myTour" :steps="steps" :callbacks="myCallbacks" />
</template>

<script>
export default {
  mounted: function () {
    this.$tours.myTour.start()
  },
}
</script>

<script setup>
const props = defineProps({
  tourName: {
    type: String,
    required: true,
    default: 'defaultTour',
  },
  steps: {
    type: Array,
    required: true,
    default: () => [],
  },
  isStoreToLocalStorage: {
    type: Boolean,
    required: false,
    default: false,
  },
})

const myCallbacks = {
  onSkip: () => {
    if (props.isStoreToLocalStorage) {
      localStorage.setItem(props.tourName, true)
    }
  },
  onFinish: () => {
    if (props.isStoreToLocalStorage) {
      localStorage.setItem(props.tourName, true)
    }
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

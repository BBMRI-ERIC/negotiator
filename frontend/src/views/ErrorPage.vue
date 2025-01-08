<template>
  <div class="error-page d-flex flex-column justify-content-center align-items-center">
    <h1 class="text-title text-uppercase">
      Oops! Sorry something went wrong.
    </h1>
    <div class="image-text-box mt-5">
      <div class="errors-text">
        <p 
          v-for="(error, index) in useNotifications.allNotifications"
          :key="index"
        >
          {{ error.message }}
        </p>
      </div>
      <img class="image" :src="errorImage" alt="error image">
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '../store/user'
import { useNotificationsStore } from '../store/notifications'
import errorImage from "../assets/images/error-image.jpg"

const userStore = useUserStore()
const useNotifications = useNotificationsStore()

onMounted(async () => {
  if (Object.keys(userStore.userInfo).length === 0) {
    await userStore.retrieveUser()
  }
})
</script>

<style scoped>
.text-title {
  position: absolute;
  top: 150px;
  width: auto;
  text-wrap: nowrap;
}
.image-text-box {
  display: flex;
  flex-direction: row;
  align-items: center;
}
.errors-text {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 15vh 0 0 75px;
  color: #f35353;
}
.image {
  width: 50vw;
}
@media screen and (max-width: 480px) {
  .image {
    display: none;
  }
  .text-title {
    text-wrap: wrap;
  }
  .errors-text {
    margin-left: 0;
  }
}
</style>

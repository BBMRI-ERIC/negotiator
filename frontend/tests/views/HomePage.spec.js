// import { beforeEach, describe, expect, it, vi } from "vitest"
// import { shallowMount } from "@vue/test-utils"
// import { createStore } from "vuex"
// import HomePage from "../../src/views/HomePage.vue"

// describe("HomePage", () => {
//   let store
//   let auth

//   beforeEach(() => {
//     store = createStore()
//     auth = {
//       isUserLoggedIn: vi.fn()
//     }
//   }),
//   it("contains jumbotron if request id is not present", () => {
//     const mockRoute = {
//       params: {
//         requestId: undefined
//       }
//     }

//     const wrapper = shallowMount(HomePage, {
//       global: {
//         mocks: {
//           $route: mockRoute,
//           $auth: auth
//         },
//         plugins: [store]
//       }
//     })
//     expect(wrapper.html()).toContain("class=\"jumbotron\"")
//   }),
//   it("contains a negotiation for if request is present", () => {
//     const mockRoute = {
//       params: {
//         requestId: "aaabbbccc"
//       }
//     }

//     const wrapper = shallowMount(HomePage, {
//       global: {
//         mocks: {
//           $route: mockRoute,
//           $auth: auth
//         },
//         plugins: [store]
//       }
//     })
//     expect(wrapper.html()).toContain("<negotiation-form")
//   }),
//   it("shows a notification div if notification is present", () => {
//     const message = "Something to notify"
//     store = createStore({
//       getters: {
//         getNotification () {
//           return message
//         }
//       }
//     })
//     const mockRoute = {
//       params: {
//         requestId: "aaabbbccc"
//       }
//     }

//     const wrapper = shallowMount(HomePage, {
//       global: {
//         mocks: {
//           $route: mockRoute,
//           $auth: auth
//         },
//         plugins: [store]
//       }
//     })
//     expect(wrapper.html()).toContain("class=\"alert")
//     expect(wrapper.html()).toContain(message)
//   })
// })

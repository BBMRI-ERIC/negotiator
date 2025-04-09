import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ResourceItem from '@/components/ResourceItem.vue'

describe('ResourceItem.vue', () => {
  // Sample resource with lifecycle, submission, and requirement links.
  const resource = {
    id: '1',
    name: 'Test Resource',
    sourceId: 'test:resource:1',
    _links: {
      nextLifecycle: { title: 'Next Lifecycle event', href: 'http://example.com/update', name: 'Update' },
      'submission-1': { title: 'Submission', href: 'http://example.com/submit', name: 'Submit' },
      'requirement-1': { title: 'Requirement', href: 'http://example.com/require', name: 'Requirement' }
    },
    currentState: 'state1'
  }

  const uiConfiguration = {
    primaryTextColor: '#000000',
    secondaryTextColor: '#ffffff'
  }

  it('emits "update-resource-state" when lifecycle link is clicked', async () => {
    const wrapper = mount(ResourceItem, {
      props: {
        resource,
        uiConfiguration
      }
    })

    // Find the lifecycle update link using its CSS class.
    const lifecycleLink = wrapper.find('.lifecycle-links a')
    await lifecycleLink.trigger('click')

    // Expect that the "update-resource-state" event is emitted with the update URL.
    expect(wrapper.emitted()['update-resource-state']).toBeTruthy()
    const emittedEvent = wrapper.emitted()['update-resource-state'][0]
    expect(emittedEvent).toEqual(['http://example.com/update'])
  })

  it('emits "open-form-modal" when submission link is clicked', async () => {
    const wrapper = mount(ResourceItem, {
      props: {
        resource,
        uiConfiguration
      }
    })

    // Find the submission link.
    const submissionLink = wrapper.find('.submission-text')
    await submissionLink.trigger('click.prevent')

    // Expect that the "open-form-modal" event is emitted with the submission link URL.
    expect(wrapper.emitted()['open-form-modal']).toBeTruthy()
    const emittedEvent = wrapper.emitted()['open-form-modal'][0]
    expect(emittedEvent).toEqual(['http://example.com/submit'])
  })

  it('emits "open-modal" when requirement link is clicked', async () => {
    const wrapper = mount(ResourceItem, {
      props: {
        resource,
        uiConfiguration
      }
    })

    // Find the missing requirement link.
    const requirementLink = wrapper.find('.requirement-text')
    await requirementLink.trigger('click')

    // Expect that the "open-modal" event is emitted with the requirement link URL and the resource ID.
    expect(wrapper.emitted()['open-modal']).toBeTruthy()
    const emittedEvent = wrapper.emitted()['open-modal'][0]
    expect(emittedEvent).toEqual(['http://example.com/require', resource.id])
  })
})

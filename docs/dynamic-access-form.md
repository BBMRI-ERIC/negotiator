# Dynamic Access Form

Our platform provides a **Dynamic Access Form** that adapts to user needs in real-time. This feature allows users to
submit requests efficiently by presenting a form that adjusts based on previous inputs and selections. This ensures that
users only see relevant fields, enhancing the overall experience and reducing the likelihood of errors.

## Form Structure

An access form does have required and optional sections to allow for tailored data collection for an access request.

### Required Information

Each access form must include at least the name for identification and one section for the requestor to provide additional information about the request.

### Optional Information

Additional sections can be added to the access form to collect more detailed information about the request, or specific requirements.

### Access Form Validation

In order to allow the definition of access forms and elements the XSD file [bbmri-eric_negotiator_access_form.xsd](./bbmri-eric_negotiator_access_form.xsd) is used to validate the access form setup. The XSD file is used to validate the access form setup definitions using XML, but can be transformed to validate JSON or other formats.

An example setup of the BBMRI-ERIC access form is provided in the XML file [bbmri-eric_access_form.xml](./bbmri-eric_access_form.xml).

## Access Form Elements

The **Dynamic Access Form** supports various data field formats to accommodate different types of input:

- **TEXT**: A single-line text for short strings, such as names or titles.
- **TEXT_LARGE**: A multi-line text for longer strings, such as descriptions or comments.
- **NUMBER**: An field that accepts numerical values, such as quantities or measurements.
- **DATE**: A date field that allows users to input a calendar date.
- **BOOLEAN**: A boolean input that allows users to select or deselect an option.
- **SINGLE_CHOICE**: A set of options where only one can be selected at a time, useful for multiple-choice questions.
- **MULTIPLE_CHOICE**: A dropdown menu that provides a list of options for users to choose from.
- **FILE**: An input field that allows users to upload files, such as documents or images.
- **INFORMATION**: A read-only field that displays information or instructions to users.

This formats ensure that the form can handle a wide range of data types and user inputs effectively.
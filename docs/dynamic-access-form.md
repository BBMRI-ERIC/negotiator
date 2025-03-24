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

In order to allow the definition of access forms and elements the XSD file **bbmri-eric_negotiator_access_form.xsd** is used to validate the access form setup. The XSD file is used to validate the access form setup definitions using XML, but can be transformed to validate JSON or other formats.

<details open>
  <summary>bbmri-eric_negotiator_access_form.xsd</summary>

  ```xml
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
    <!-- The definition of elements in an access form -->
    <xs:element name="access_form" type="access_form"/>
    <!-- The definition of the access form -->
    <xs:complexType name="access_form">
        <xs:sequence>
            <xs:element name="name" type="name"/>
            <xs:element name="form_section" type="form_section" minOccurs="1" maxOccurs="unbounded"/>
        </xs:sequence>
   </xs:complexType>
    <!-- The definition of the access form name -->
    <xs:simpleType name="name">
        <xs:restriction base="xs:string">
        <xs:maxLength value="255"/>
        </xs:restriction>
    </xs:simpleType>
    <!-- The definition of the access form section label -->
    <xs:simpleType name="label">
        <xs:restriction base="xs:string">
        <xs:maxLength value="255"/>
        </xs:restriction>
    </xs:simpleType>
    <!-- The definition of the access form section description -->
    <xs:simpleType name="description">
        <xs:restriction base="xs:string">
            <xs:maxLength value="255"/>
        </xs:restriction>
    </xs:simpleType>
    <!-- The definition of the form section -->
    <xs:complexType name="form_section">
        <xs:sequence>
                <!-- ensure the order number is unique for the section fields -->
                        <xs:element name="order" type="xs:integer">
                            <xs:unique name="access_form_sections_uniqueOrder">
                                <xs:selector xpath="."/>
                                <xs:field xpath="@order"/>
                            </xs:unique>
                        </xs:element>
            <xs:element name="name" type="name" />
            <xs:element name="label" type="label" />
            <xs:element name="description" type="description" />
            <xs:sequence>
                <xs:element name="section_field" type="section_fields" minOccurs="1" maxOccurs="unbounded"/>
            </xs:sequence>
        </xs:sequence>
    </xs:complexType>
    <!-- The definition of the available section fields -->
    <xs:complexType name="section_fields">
        <xs:sequence>
            <!-- ensure the order number is unique for the section fields -->
            <xs:element name="order" type="xs:integer">
                <xs:unique name="section_fields_uniqueOrder">
                    <xs:selector xpath="."/>
                    <xs:field xpath="@order"/>
                </xs:unique>
            </xs:element>
            <xs:element name="field_name" type="name"/>
            <xs:element name="field_label" type="label"/>
            <xs:element name="field_description" type="description"/>
            <xs:element name="field_required" type="xs:boolean"/>
            <xs:element name="field" type="field_types"/>
        </xs:sequence>
    </xs:complexType>   
    <xs:complexType name="field_types">
        <xs:sequence>
            <xs:choice>
                <xs:element name="field_type" type="field_type"/>
            </xs:choice>
        </xs:sequence>
    </xs:complexType>
    <!-- The definition of the available field types -->
    <xs:simpleType name="field_type">
        <xs:restriction base="xs:string">
            <xs:enumeration value="TEXT"/>
            <xs:enumeration value="TEXT_LARGE"/>
            <xs:enumeration value="NUMBER"/>
            <xs:enumeration value="DATE"/>
            <xs:enumeration value="SINGLE_CHOICE"/>
            <xs:enumeration value="MULTIPLE_CHOICE"/>
            <xs:enumeration value="BOOLEAN"/>
            <xs:enumeration value="FILE"/>
            <xs:enumeration value="INFORMATION"/>
        </xs:restriction>
    </xs:simpleType>
    <!-- Type definitions for the field type restrictions -->
    <xs:simpleType name="TEXT">
        <xs:restriction base="xs:string">
            <xs:maxLength value="255"/>
        </xs:restriction>
    </xs:simpleType>
    <xs:simpleType name="TEXT_LARGE">
        <xs:restriction base="xs:string">
            <xs:maxLength value="2048"/>
        </xs:restriction>
    </xs:simpleType>
    <xs:simpleType name="DATE">
        <xs:restriction base="xs:dateTime"/>
    </xs:simpleType>
    <xs:simpleType name="NUMBER">
        <xs:restriction base="xs:decimal">
            <xs:fractionDigits value="10"/>
        </xs:restriction>
    </xs:simpleType>
    <xs:simpleType name="BOOLEAN">
        <xs:restriction base="xs:string">
            <xs:enumeration value="True"/>
            <xs:enumeration value="False"/>
        </xs:restriction>
    </xs:simpleType>
    <xs:simpleType name="SINGLE_CHOICE">
        <xs:restriction base="xs:string">
            <xs:enumeration value="CHOICE"/>
        </xs:restriction>
    </xs:simpleType>
    <xs:simpleType name="MULTIPLE_CHOICE">
        <xs:restriction base="xs:string">
            <xs:enumeration value="CHOICE_1"/>
            <xs:enumeration value="CHOICE_2"/>
            <xs:enumeration value="CHOICE_3"/>
        </xs:restriction>
    </xs:simpleType>
    <xs:simpleType name="FILE">
        <xs:restriction base="xs:string">
            <xs:maxLength value="255"/>
        </xs:restriction>
    </xs:simpleType>
    <xs:simpleType name="INFORMATION">
        <xs:restriction base="xs:string">
            <xs:maxLength value="255"/>
        </xs:restriction>
    </xs:simpleType>
</xs:schema>
```
</details>

An example setup of the BBMRI-ERIC access form is provided in the XML file **bbmri-eric_access_form.xml**

<details open>
  <summary>bbmri-eric_access_form.xml</summary>

  ```xml
<?xml-model href="bbmri-eric_negotiator_access_form.xsd" type="application/xml" schematypens="http://www.w3.org/2001/XMLSchema"?>
<access_form>
  <name>BBMRI Template</name>
    <form_section>
      <order>1</order>
      <name>project</name>
      <label>Project</label>
      <description>Provide information about your project</description>
      <section_field>
        <order>1</order>
        <field_name>title</field_name>
        <field_label>Title</field_label>
        <field_description>Give a title</field_description>
        <field_required>true</field_required>
        <field>
          <field_type>TEXT</field_type>
        </field>
      </section_field>
      <section_field>
        <order>2</order>
        <field_name>description</field_name>
        <field_label>Description</field_label>
        <field_description>Give a description</field_description>
        <field_required>true</field_required>
        <field>
          <field_type>TEXT_LARGE</field_type>
        </field>
      </section_field>
    </form_section>
    <form_section>
      <order>2</order>
      <name>request</name>
      <label>Request</label>
      <description>Provide information the resources you are requesting</description>
      <section_field>
        <order>1</order>
        <field_name>description</field_name>
        <field_label>Description</field_label>
        <field_description>Provide a request description</field_description>
        <field_required>true</field_required>
        <field>
          <field_type>TEXT_LARGE</field_type>
        </field>
      </section_field>    </form_section>
    <form_section>
      <order>3</order>
      <name>ethics_vote</name>
      <label>Ethics vote</label>
      <description>Provide information the resources you are requesting</description>
      <section_field>
        <order>1</order>
        <field_name>ethics-vote</field_name>
        <field_label>Ethics vote</field_label>
        <field_description>Write the ethics vote</field_description>
        <field_required>true</field_required>
        <field>
          <field_type>TEXT_LARGE</field_type>
        </field>
      </section_field>
            <section_field>
        <order>2</order>
        <field_name>attachment</field_name>
        <field_label>Attachment</field_label>
        <field_description>Upload ethics vote</field_description>
        <field_required>false</field_required>
        <field>
          <field_type>FILE</field_type>
        </field>
      </section_field>
    </form_section>
</access_form>
```
</details>

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
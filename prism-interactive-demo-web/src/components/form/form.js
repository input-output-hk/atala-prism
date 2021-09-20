import React from 'react';
import jsonp from 'jsonp';
import { useTranslation } from 'react-i18next';
import { Form, Input, Button, Checkbox, message } from 'antd';
import { config } from '../../app/APIs/configs';
import { emailFormatValidation } from '../../app/helpers/formRules';

import './_style.scss';

const { mailchimpURL, mailchimpU, mailchimpID } = config;

const PioneersForm = ({ form }) => {
  const { t } = useTranslation();

  const submitForm = e => {
    e.preventDefault();

    form.validateFields(['fullName', 'email', 'consent'], (errors, { fullName, email }) => {
      if (errors) return;

      const atIndex = email.indexOf('@');
      const finalEmail = `${email.slice(0, atIndex)}+ppp${email.slice(atIndex)}`;

      subscribeToNewsLetter({
        EMAIL: finalEmail,
        FULLNAME: fullName
      });
      form.resetFields();
    });
  };

  const subscribeToNewsLetter = formData => {
    jsonp(
      `${mailchimpURL}/subscribe/post-json?u=${mailchimpU}&amp;id=${mailchimpID}&${queryString.stringify(
        formData
      )}`,
      { param: 'c' },
      (err, data) => {
        if (err) return message.error(t('contact.unexpectedError'));
        if (data) {
          data.result === 'success'
            ? message.success(t('contact.mail.success'))
            : message.error(t('contact.mail.error'));
        }
      }
    );
  };

  const { getFieldDecorator } = form;

  return (
    <Form name="basic" onSubmit={submitForm} autoComplete="off">
      <Form.Item label="Full Name" name="fullName">
        {getFieldDecorator('fullName', {
          rules: [{ required: true, message: 'Please input your full name' }]
        })(<Input />)}
      </Form.Item>
      <Form.Item label="E-mail Address" name="email">
        {getFieldDecorator('email', {
          rules: [
            {
              validator: (_, value, cb) => emailFormatValidation(value, cb),
              message: 'Please input a valid email address',
              required: true
            }
          ]
        })(<Input />)}
      </Form.Item>
      <Form.Item name="consent">
        {getFieldDecorator('consent', {
          rules: [
            {
              required: true,
              message: 'You must accept the Terms & Conditions and the Privacy Policy'
            }
          ]
        })(
          <Checkbox>
            Yes, I have read and agree to the Atala PRISM{' '}
            <a
              href="https://legal.atalaprism.io/terms-and-conditions.html "
              target="_blank"
              rel="noopener noreferrer"
              className="link"
            >
              Terms & Conditions
            </a>
            {' and '}
            <a
              href="https://legal.atalaprism.io/privacy-policy.html "
              target="_blank"
              rel="noopener noreferrer"
              className="link"
            >
              Privacy Policy.
            </a>
          </Checkbox>
        )}
      </Form.Item>
      <Form.Item wrapperCol={{ offset: 8, span: 16 }}>
        <Button type="primary" htmlType="submit">
          Submit interest
        </Button>
      </Form.Item>
    </Form>
  );
};

export default Form.create({ name: 'pionners' })(PioneersForm);

import React from 'react';
import jsonp from 'jsonp';
import queryString from 'query-string';
import { Form, Input, Button, Checkbox, message } from 'antd';
import { config } from '../../app/APIs/configs';
import { emailFormatValidation } from '../../app/helpers/formRules';
import Logger from '../../app/helpers/Logger';

import './_style.scss';

const { mailchimpURL, mailchimpU, mailchimpID } = config;

const PioneersForm = ({ form }) => {
  const submitForm = e => {
    e.preventDefault();

    form.validateFields(['fullName', 'email', 'consent'], (errors, { fullName, email }) => {
      if (errors) return;

      subscribeToNewsLetter({
        EMAIL: email,
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
        if (err) {
          Logger.error('There has been an error', err);
          return message.error('There has been an error');
        }
        if (data) {
          if (data.result === 'success') message.success('Thanks for registering!');
          else message.error('Your email is already registered');
        }
      }
    );
  };

  const { getFieldDecorator } = form;

  return (
    <Form name="basic" onSubmit={submitForm} autoComplete="off">
      <Form.Item label="Full name" colon={false} name="fullName">
        {getFieldDecorator('fullName', {
          rules: [{ required: true, message: 'Please input your full name' }]
        })(<Input />)}
      </Form.Item>
      <Form.Item label="E-mail address" colon={false} name="email">
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
            Yes, I have read and agreed to the Atala PRISM{' '}
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
          Submit
        </Button>
      </Form.Item>
    </Form>
  );
};

export default Form.create({ name: 'pionners' })(PioneersForm);

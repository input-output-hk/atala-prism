import * as React from 'react';
import { Form, Input, Button, Checkbox } from 'antd';
import './_style.scss';

const FormPioneers = () => {
  const onFinish = () => {
    console.log('Success:', values);
  };

  const onFinishFailed = () => {
    console.log('Failed:', errorInfo);
  };

  return (
    <Form
      name="basic"
      initialValues={{ remember: true }}
      onFinish={onFinish}
      onFinishFailed={onFinishFailed}
      autoComplete="off"
    >
      <Form.Item className="form">
        <Form.Item
          label="Full Name"
          name="username"
          rules={[{ required: true, message: 'Please input your username!' }]}
        >
          <Input />
        </Form.Item>
        <Form.Item
          label="E-mail Address"
          name="password"
          rules={[{ required: true, message: 'Please input your password!' }]}
        >
          <Input.Password />
        </Form.Item>
        <Form.Item name="remember" valuePropName="checked" wrapperCol={{ offset: 8, span: 16 }}>
          <Checkbox>
            Yes, I have read and agree to the{' '}
            <a href="#" className="link">
              Atala PRISM Terms
            </a>{' '}
            &{' '}
            <a href="#" className="link">
              Conditions and Privacy Policy.
            </a>
          </Checkbox>
        </Form.Item>

        <Form.Item wrapperCol={{ offset: 8, span: 16 }}>
          <Button type="primary" htmlType="submit">
            Submit interest
          </Button>
        </Form.Item>
      </Form.Item>
    </Form>
  );
};

export default FormPioneers;

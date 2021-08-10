import * as React from 'react';
import { Tabs, Input, Select } from 'antd';
import facebook from '../../images/facebook.svg';
import twitter from '../../images/twitter.svg';
import heart from '../../images/heart.svg';
import user from '../../images/user.svg';
import CustomButton from '../../app/components/common/Atoms/CustomButton/CustomButton';
import './_style.scss';

const { Option } = Select;

function onBlur() {
  console.log('blur');
}

function onFocus() {
  console.log('focus');
}

function onSearch(val) {
  console.log('search:', val);
}

const { TabPane } = Tabs;

const { TextArea } = Input;

function callback(key) {
  console.log(key);
}

const onChange = e => {
  console.log('Change:', e.target.value);
};

const TabsComment = () => {
  return (
    <div className="Tabs">
      <Tabs defaultActiveKey="1" onChange={callback}>
        <TabPane tab="Comments" key="1">
          <div className="controlers">
            <div className="btnSection">
              <div>
                <p>Recommend</p>
              </div>
              <div className="img">
                <img src={heart} alt="heart" />
              </div>
              <div className="likeCount">
                <p>3</p>
              </div>
            </div>
            <div className="btnSection">
              <div>
                <p>Share with</p>
              </div>
              <div className="img">
                <img src={facebook} alt="facebook" />
              </div>
              <div className="img">
                <img src={twitter} alt="twitter" />
              </div>
            </div>
            <div className="btnSection">
              <Select
                showSearch
                style={{ width: 200 }}
                placeholder="Sort By"
                optionFilterProp="children"
                onChange={onChange}
                onFocus={onFocus}
                onBlur={onBlur}
                onSearch={onSearch}
                filterOption={(input, option) =>
                  option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }
              >
                <Option value="jack">Best</Option>
                <Option value="lucy">All</Option>
              </Select>
            </div>
          </div>
          <div className="comments">
            <div className="user">
              <img src={user} alt="user" />
            </div>
            <TextArea showCount maxLength={100} onChange={onChange} />
            <CustomButton buttonProps={{ className: 'theme-primary' }} buttonText="Submit" />
          </div>
          <div className="comments column">
            <div className="user">
              <img src={user} alt="user" />
              <p>User Name</p>
            </div>
            <div className="commentBox">
              <p>Lorem ipsum dolor sit amet.</p>
            </div>
          </div>
          <div className="comments column">
            <div className="user">
              <img src={user} alt="user" />
              <p>User Name</p>
            </div>
            <div className="commentBox">
              <p>Lorem ipsum dolor sit amet.</p>
            </div>
          </div>
        </TabPane>
        <TabPane tab="Input Output Blog" key="2">
          Content of Tab Pane 2
        </TabPane>
      </Tabs>
    </div>
  );
};

export default TabsComment;

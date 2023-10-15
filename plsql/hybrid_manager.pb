create context hybrid_cache using hybrid_manager accessed globally;

create or replace package body hybrid_manager as

  c_context                constant varchar2(13) := 'hybrid_cache';

  gc_login_id_param        constant varchar2(9)  := 'LOGINID';
  gc_default_user          constant varchar2(30) := 'HYBRID-DEMO';

  c_datetime_format_mask   constant varchar2(21) := 'dd.mm.yyyy hh24:mi:ss';
  c_number_format_mask     constant varchar2(63) := '99999999999999999999999999999999999D999999999999999999999999999';
  c_nls_numeric_characters constant varchar2(2)  := ',.';

  procedure setup_hybrid_demo(p_name in varchar2, 
                              p_value in varchar2) is
  begin
    update tab_hybrid_demo_setup
       set value = p_value
     where name = p_name;

    if sql%notfound then
      insert into tab_hybrid_demo_setup(name, value)
           values (p_name, p_value);
    end if;
  end setup_hybrid_demo;

  function get_hybrid_setup_value(p_name in varchar2) return varchar2 result_cache relies_on (tab_hybrid_demo_setup) is
    w_value varchar2(1000);
  begin
    begin
      select value
        into w_value
        from tab_hybrid_demo_setup
       where name = p_name;
    exception when no_data_found then
      w_value := null;
    end;

    return w_value;
  end get_hybrid_setup_value;

  function get_client_id return varchar2 is
  begin
    return sys_context('USERENV', 'CLIENT_IDENTIFIER');
  end get_client_id;

  procedure set_context_client_id(p_login_id in number default null) is
    w_curr_client_id varchar2(32767);
    w_new_login_id   number;
  begin
    w_new_login_id   := nvl(p_login_id, nvl(apex_util.get_numeric_session_state(gc_login_id_param), hybrid_manager.g_login_id));
    w_curr_client_id := get_client_id();

    if to_char(w_new_login_id) != w_curr_client_id or w_curr_client_id is null then
      dbms_session.set_identifier(w_new_login_id);
    end if;
  end set_context_client_id;

  function get_base_url return varchar2 is
    w_base_url varchar2(1000);
  begin
    w_base_url := get_hybrid_setup_value(hybrid_manager.gc_setup_base_apex_url);
    if w_base_url is null then
      raise_application_error(-20001, 'Apex base url has to be defined in the hybrid demo setup table!');
    else
      return w_base_url;
    end if;
  end get_base_url;

  function get_apex_app_page_string return varchar2 is
    w_app_id varchar2(100);
    w_page_id varchar2(100);
  begin
    w_app_id  := get_hybrid_setup_value(hybrid_manager.gc_setup_apex_app_id);
    w_page_id := get_hybrid_setup_value(hybrid_manager.gc_setup_apex_page_id);

    if w_app_id is null or w_page_id is null then
      raise_application_error(-20001, 'Apex base app and page have to be defined in the hybrid demo setup table!');
    else
      return w_app_id || ':' || w_page_id;
    end if;
    
  end get_apex_app_page_string;

  function extract_login_id_from_url return varchar2 is
  begin
    return replace(upper(owa_util.get_cgi_env('QUERY_STRING')), gc_login_id_param || '=');
  end extract_login_id_from_url;

  procedure validate_login_id(p_login_id in number) is
    w_id number;
  begin
    begin
      select id
        into w_id
        from tab_hybrid_demo_login
       where id = p_login_id;
    exception when no_data_found then
      raise_application_error(-20000, 'Invalid login id!');
    end;
  end validate_login_id;

  function new_login return number as
    pragma autonomous_transaction;
    w_login_id number;
  begin
    if hybrid_manager.g_login_id is null then
      select seq_hybrid_demo_login.nextval
        into w_login_id
        from dual;
      insert into tab_hybrid_demo_login(id, created_at) 
           values (w_login_id, sysdate);
      commit;

      hybrid_manager.g_login_id := w_login_id;
    end if;

    dbms_session.set_identifier(hybrid_manager.g_login_id);
  
    return hybrid_manager.g_login_id;
  end new_login;

  function apex_sentry return boolean is
    w_login_id            varchar2(32000);
    w_logged_in           boolean         := false;
  begin
    if apex_application.g_user <> 'nobody' then
      w_logged_in := true;
    end if;

    if not w_logged_in then
      w_login_id := extract_login_id_from_url;

      if w_login_id is not null then
        validate_login_id(to_number(w_login_id));

        apex_custom_auth.login(
          p_uname      => gc_default_user,
          p_password   => w_login_id,
          p_session_id => v('APP_SESSION'),
          p_app_page   => get_apex_app_page_string);

        apex_util.set_session_state(gc_login_id_param, w_login_id);

        w_logged_in := true;
      end if;
    end if;

    return w_logged_in;
  end apex_sentry;

  function apex_auth(p_username in varchar2,
                     p_password in varchar2) return boolean is
    w_login_id  varchar2(32000);
    w_logged_in boolean         := false;
  begin
    w_login_id := extract_login_id_from_url;
  
    if w_login_id is not null then
      apex_util.set_session_state(gc_login_id_param, w_login_id);

      w_logged_in := true;
    else
      w_logged_in := apex_util.is_login_password_valid(p_username => p_username,
                                                       p_password => p_password);
    end if;
  
    return w_logged_in;
  end apex_auth;

  function get_apex_url(p_login_id in number) return varchar2 is
    w_url varchar2(32000);
  begin
    w_url := get_base_url || '?loginId=' || to_char(nvl(p_login_id, hybrid_manager.g_login_id));

    return replace(replace(w_url,chr(10)),chr(13));
  end get_apex_url;

  function get_apex_url return varchar2 is
  begin
    return get_apex_url(hybrid_manager.g_login_id);
  end get_apex_url;

  procedure apex_init is
    w_apex_login_id number;
  begin
    w_apex_login_id := apex_util.get_numeric_session_state(gc_login_id_param);

    hybrid_manager.g_login_id := w_apex_login_id;
    dbms_application_info.set_client_info(w_apex_login_id);
  end apex_init;

  procedure apex_cleanup is
  begin
    null;
  end apex_cleanup;

  -- CACHE - SYS CONTEXT
  procedure cache_set_n(p_parameter in varchar2, p_value in number) is
  begin
    cache_set_c(p_parameter => p_parameter,
                p_value     => to_char(p_value, c_number_format_mask, 'NLS_NUMERIC_CHARACTERS=''' || c_nls_numeric_characters || ''''));
  end cache_set_n;

  procedure cache_set_d(p_parameter in varchar2, p_value in date) is
  begin
    cache_set_c(p_parameter => p_parameter,
                p_value     => to_char(p_value, c_datetime_format_mask));
  end cache_set_d;

  procedure cache_set_c(p_parameter in varchar2, p_value in varchar2) is
    w_client_id varchar2(32767);
  begin
    set_context_client_id();
    w_client_id := get_client_id();
    
    dbms_session.set_context(namespace => c_context,
                             attribute => p_parameter,
                             value     => p_value,
                             username  => null,
                             client_id => w_client_id);
  end cache_set_c;

  function cache_get_n(p_parameter in varchar2, p_login_id in number default null) return number is
  begin
    return to_number(cache_get_c(p_parameter, p_login_id), c_number_format_mask, 'NLS_NUMERIC_CHARACTERS=''' || c_nls_numeric_characters || '''');
  end cache_get_n;

  function cache_get_d(p_parameter in varchar2, p_login_id in number default null) return date is
  begin
    return to_date(cache_get_c(p_parameter, p_login_id), c_datetime_format_mask);
  end cache_get_d;

  function cache_get_c(p_parameter in varchar2, p_login_id in number default null) return varchar2 is
    w_ret varchar2(32767);
    w_client_id varchar2(32767);
  begin
    set_context_client_id(p_login_id);
    w_client_id := get_client_id();

    w_ret := sys_context(c_context,
                         p_parameter,
                         4000);
    dbms_session.set_identifier(w_client_id);
    return w_ret;
  end cache_get_c;

  procedure cache_clear(p_parameter in varchar2 default null, p_login_id in number default null) is
    w_client_id varchar2(32767);
  begin
    set_context_client_id(p_login_id);
    w_client_id := get_client_id();
    if p_parameter is null then
      dbms_session.clear_context(namespace => c_context,
                                 client_id => w_client_id);
    else
      dbms_session.clear_context(namespace => c_context,
                                 attribute => p_parameter,
                                 client_id => w_client_id);
    end if;
  end cache_clear;

end hybrid_manager;
/
show errors package body hybrid_manager;

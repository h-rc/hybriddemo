begin
  hybrid_manager.setup_hybrid_demo(hybrid_manager.gc_setup_base_apex_url, 'BASE HYBRID DEMO APEX URL');
  hybrid_manager.setup_hybrid_demo(hybrid_manager.gc_setup_apex_app_id,   'APPLICATION ID');
  hybrid_manager.setup_hybrid_demo(hybrid_manager.gc_setup_apex_page_id,  'PAGE ID');
end;
/
show errors;
